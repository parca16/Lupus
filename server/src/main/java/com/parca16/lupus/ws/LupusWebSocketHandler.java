package com.parca16.lupus.ws;

import static com.parca16.lupus.ws.SessionAttributes.CONNECTION_ID;
import static com.parca16.lupus.ws.SessionAttributes.NICKNAME;
import static com.parca16.lupus.ws.SessionAttributes.PLAYER_ID;
import static com.parca16.lupus.ws.SessionAttributes.ROOM_CODE;

import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.parca16.lupus.game.CupidAction;
import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GameParticipant;
import com.parca16.lupus.game.GameRegistry;
import com.parca16.lupus.game.GameRuleException;
import com.parca16.lupus.game.GuardianAction;
import com.parca16.lupus.game.NightAction;
import com.parca16.lupus.game.SeerAction;
import com.parca16.lupus.game.WitchAction;
import com.parca16.lupus.game.WitchPotion;
import com.parca16.lupus.game.WolfVote;
import com.parca16.lupus.protocol.CreateRoom;
import com.parca16.lupus.protocol.Echo;
import com.parca16.lupus.protocol.ErrorMessage;
import com.parca16.lupus.protocol.GameAction;
import com.parca16.lupus.protocol.GameStarting;
import com.parca16.lupus.protocol.Hello;
import com.parca16.lupus.protocol.JoinRoom;
import com.parca16.lupus.protocol.KickPlayer;
import com.parca16.lupus.protocol.Kicked;
import com.parca16.lupus.protocol.LeaveRoom;
import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Ping;
import com.parca16.lupus.protocol.Pong;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.protocol.ResumeRoom;
import com.parca16.lupus.protocol.RoomSettings;
import com.parca16.lupus.protocol.RoomState;
import com.parca16.lupus.protocol.StartGame;
import com.parca16.lupus.protocol.UpdateSettings;
import com.parca16.lupus.protocol.Welcome;
import com.parca16.lupus.room.GameSettings;
import com.parca16.lupus.room.GameState;
import com.parca16.lupus.room.Room;
import com.parca16.lupus.room.RoomException;
import com.parca16.lupus.room.RoomPlayer;
import com.parca16.lupus.room.RoomRegistry;

/**
 * Handshake, eco y gestion de salas por WebSocket.
 *
 * <p>Al abrir la conexion el servidor manda un {@code welcome} con el identificador de conexion.
 * Las salas se guardan en memoria y su lista de jugadores se difunde a todos sus miembros.
 */
@Component
public class LupusWebSocketHandler extends TextWebSocketHandler {

	private final RoomRegistry rooms;
	private final PlayerSessions sessions;
	private final RoomNotifier notifier;
	private final GameRegistry games;
	private final GameBroadcaster broadcaster;
	private final Clock clock;

	public LupusWebSocketHandler(RoomRegistry rooms, PlayerSessions sessions, RoomNotifier notifier,
			GameRegistry games, GameBroadcaster broadcaster, Clock clock) {
		this.rooms = rooms;
		this.sessions = sessions;
		this.notifier = notifier;
		this.games = games;
		this.broadcaster = broadcaster;
		this.clock = clock;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		session.getAttributes().put(CONNECTION_ID, UUID.randomUUID().toString());
		session.getAttributes().put(NICKNAME, "");
		sendWelcome(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String playerId = (String) session.getAttributes().get(PLAYER_ID);
		String roomCode = (String) session.getAttributes().get(ROOM_CODE);

		if (playerId == null || roomCode == null) {
			return;
		}

		sessions.unbind(playerId);
		rooms.find(roomCode).ifPresent(room -> {
			if (room.contains(playerId)) {
				room.disconnect(playerId);
				notifier.broadcastPlayers(room, playerId);
			}
		});
		SessionAttributes.clearMembership(session);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		Protocol.Envelope envelope;
		try {
			envelope = Protocol.peek(message.getPayload());
		}
		catch (RuntimeException exception) {
			sendError(session, "invalid_message", "El mensaje no es JSON valido.");
			return;
		}

		if (envelope.v() != Protocol.VERSION) {
			sendError(session, "unsupported_version",
					"Version de protocolo no soportada: " + envelope.v());
			return;
		}

		Class<? extends Message> target = Protocol.types().get(envelope.type());
		if (target == null) {
			sendError(session, "unknown_type", "Tipo de mensaje desconocido: " + envelope.type());
			return;
		}

		Message decoded;
		try {
			decoded = Protocol.decode(message.getPayload(), target);
		}
		catch (RuntimeException exception) {
			sendError(session, "invalid_message", "El mensaje no cumple el contrato del protocolo.");
			return;
		}

		marcarActividad(session);

		try {
			dispatch(session, decoded);
		}
		catch (RoomException exception) {
			sendError(session, exception.code(), exception.getMessage());
		}
		catch (GameRuleException exception) {
			sendError(session, exception.code(), exception.getMessage());
		}
	}

	/** Cualquier mensaje de un jugador de la sala cuenta como actividad (issue #25). */
	private void marcarActividad(WebSocketSession session) {
		String roomCode = (String) session.getAttributes().get(ROOM_CODE);
		if (roomCode != null) {
			rooms.find(roomCode).ifPresent(Room::touch);
		}
	}

	private void dispatch(WebSocketSession session, Message message) {
		switch (message) {
			case Hello hello -> {
				session.getAttributes().put(NICKNAME, hello.nickname());
				sendWelcome(session);
			}
			case Echo echo -> notifier.send(session,
					new Echo(Protocol.VERSION, echo.id(), echo.at(), echo.text()));
			case Ping ping -> notifier.send(session,
					new Pong(Protocol.VERSION, ping.id(), System.currentTimeMillis()));
			case CreateRoom createRoom -> crearSala(session, createRoom);
			case JoinRoom joinRoom -> unirseASala(session, joinRoom);
			case ResumeRoom resumeRoom -> reanudarSala(session, resumeRoom);
			case LeaveRoom ignored -> salirDeSala(session);
			case KickPlayer kickPlayer -> expulsar(session, kickPlayer);
			case UpdateSettings updateSettings -> actualizarConfiguracion(session, updateSettings);
			case StartGame ignored -> empezarPartida(session);
			case GameAction gameAction -> ejecutarAccion(session, gameAction);
			default -> sendError(session, "unknown_type", "Mensaje no admitido del cliente.");
		}
	}

	private void crearSala(WebSocketSession session, CreateRoom message) {
		Room room = rooms.create();
		RoomPlayer player = room.addPlayer(message.nickname());

		vincular(session, room, player);
		sendRoomState(session, room, player);
		sendRoomSettings(session, room);
	}

	private void unirseASala(WebSocketSession session, JoinRoom message) {
		Room room = rooms.find(message.roomCode()).orElseThrow(() -> new RoomException(
				RoomException.ROOM_NOT_FOUND, "No existe ninguna sala con el codigo " + message.roomCode()));

		if (room.state() != GameState.WAITING) {
			throw new RoomException(RoomException.GAME_ALREADY_STARTED,
					"La partida ya ha empezado; no se puede entrar.");
		}

		RoomPlayer player = room.addPlayer(message.nickname());

		vincular(session, room, player);
		sendRoomState(session, room, player);
		sendRoomSettings(session, room);
		notifier.broadcastPlayers(room, player.id());
	}

	/** Vuelve a ocupar un asiento con la identidad persistente del navegador. */
	private void reanudarSala(WebSocketSession session, ResumeRoom message) {
		Room room = rooms.find(message.roomCode()).orElseThrow(() -> new RoomException(
				RoomException.ROOM_NOT_FOUND, "La sala ya no existe."));

		RoomPlayer player = room.reconnect(message.playerId());

		vincular(session, room, player);
		sendRoomState(session, room, player);
		sendRoomSettings(session, room);
		notifier.broadcastPlayers(room, player.id());
		games.find(room.code()).ifPresent(game -> broadcaster.sendState(game, player.id()));
	}

	private void actualizarConfiguracion(WebSocketSession session, UpdateSettings message) {
		Room room = salaActual(session);

		if (!room.isHost(jugadorActual(session))) {
			throw new RoomException(RoomException.NOT_HOST, "Solo el host puede cambiar la configuracion.");
		}

		GameSettings settings = new GameSettings(message.wolves(), message.roles(), message.lives(),
				message.president(), message.debateSeconds(), message.roleSeconds(), message.voteSeconds(),
				message.showVoteDetail());

		room.gameSettings(settings);
		notifier.broadcastAll(room, roomSettingsMessage(room));
	}

	private void empezarPartida(WebSocketSession session) {
		Room room = salaActual(session);

		if (!room.isHost(jugadorActual(session))) {
			throw new RoomException(RoomException.NOT_HOST, "Solo el host puede empezar la partida.");
		}
		if (games.find(room.code()).isPresent()) {
			throw new RoomException(RoomException.GAME_ALREADY_STARTED, "La partida ya ha empezado.");
		}

		int conectados = (int) room.players().stream().filter(RoomPlayer::connected).count();
		room.gameSettings().validatedForStart(conectados);

		List<GameParticipant> participants = room.players().stream()
				.filter(RoomPlayer::connected)
				.map(player -> new GameParticipant(player.id(), player.nickname()))
				.toList();

		Game game = Game.start(room.code(), room.gameSettings(), participants, clock, new Random());
		games.put(game);
		room.state(GameState.STARTING);

		notifier.broadcastAll(room, new GameStarting(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), room.code()));
		broadcaster.broadcastState(game);
	}

	private void salirDeSala(WebSocketSession session) {
		Room room = salaActual(session);
		String playerId = jugadorActual(session);

		if (games.find(room.code()).isPresent()) {
			throw new RoomException(RoomException.GAME_ALREADY_STARTED,
					"No se puede salir con la partida en curso.");
		}

		if (room.isHost(playerId)) {
			notifier.close(room, RoomNotifier.HOST_LEFT, playerId);
			SessionAttributes.clearMembership(session);
			sessions.unbind(playerId);
			return;
		}

		room.remove(playerId);
		SessionAttributes.clearMembership(session);
		sessions.unbind(playerId);
		notifier.broadcastPlayers(room, playerId);
	}

	private void expulsar(WebSocketSession session, KickPlayer message) {
		Room room = salaActual(session);
		String playerId = jugadorActual(session);

		if (games.find(room.code()).isPresent()) {
			throw new RoomException(RoomException.GAME_ALREADY_STARTED,
					"No se puede expulsar con la partida en curso.");
		}

		if (!room.isHost(playerId)) {
			throw new RoomException(RoomException.NOT_HOST, "Solo el host puede expulsar jugadores.");
		}
		if (playerId.equals(message.playerId())) {
			throw new RoomException(RoomException.NOT_HOST, "El host no puede expulsarse a si mismo.");
		}

		RoomPlayer expulsado = room.remove(message.playerId());
		sessions.find(expulsado.id()).ifPresent(target -> {
			notifier.send(target, new Kicked(Protocol.VERSION, UUID.randomUUID().toString(),
					System.currentTimeMillis(), room.code()));
			SessionAttributes.clearMembership(target);
			sessions.unbind(expulsado.id());
		});
		notifier.broadcastPlayers(room, expulsado.id());
	}

	private Room salaActual(WebSocketSession session) {
		String roomCode = (String) session.getAttributes().get(ROOM_CODE);
		return rooms.find(roomCode).orElseThrow(
				() -> new RoomException(RoomException.NOT_IN_ROOM, "No estas en ninguna sala."));
	}

	private static String jugadorActual(WebSocketSession session) {
		String playerId = (String) session.getAttributes().get(PLAYER_ID);
		if (playerId == null) {
			throw new RoomException(RoomException.NOT_IN_ROOM, "No estas en ninguna sala.");
		}
		return playerId;
	}

	private void vincular(WebSocketSession session, Room room, RoomPlayer player) {
		session.getAttributes().put(PLAYER_ID, player.id());
		session.getAttributes().put(ROOM_CODE, room.code());
		session.getAttributes().put(NICKNAME, player.nickname());
		sessions.bind(player.id(), session);
	}

	private void sendRoomState(WebSocketSession session, Room room, RoomPlayer player) {
		notifier.send(session, new RoomState(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), room.code(), player.id(), RoomNotifier.wire(room.players())));
	}

	private void sendRoomSettings(WebSocketSession session, Room room) {
		notifier.send(session, roomSettingsMessage(room));
	}

	private static RoomSettings roomSettingsMessage(Room room) {
		GameSettings settings = room.gameSettings();
		return new RoomSettings(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				room.code(), settings.wolves(), settings.roles(), settings.lives(), settings.president(),
				settings.debateSeconds(), settings.roleSeconds(), settings.voteSeconds(),
				settings.showVoteDetail());
	}

	private void sendWelcome(WebSocketSession session) {
		long now = System.currentTimeMillis();
		notifier.send(session, new Welcome(Protocol.VERSION, UUID.randomUUID().toString(), now,
				(String) session.getAttributes().get(CONNECTION_ID),
				(String) session.getAttributes().get(NICKNAME), now));
	}

	private void sendError(WebSocketSession session, String code, String message) {
		notifier.send(session, new ErrorMessage(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), code, message));
	}

	private void ejecutarAccion(WebSocketSession session, GameAction message) {
		Room room = salaActual(session);
		Game game = games.find(room.code()).orElseThrow(
				() -> new RoomException(RoomException.NOT_IN_ROOM, "No hay partida en curso."));
		String playerId = jugadorActual(session);

		String target = emptyToNull(message.targetId());

		switch (message.action()) {
			case "president_vote" -> game.submitPresidentVote(playerId, target);
			case "ready" -> game.markReadyToVote(playerId);
			case "vote" -> game.submitVote(playerId, target);
			case "knight_choice" -> game.chooseKnightTarget(playerId, target);
			case "night_action" -> game.submitNightAction(playerId, nightAction(message, target));
			default -> throw new GameRuleException(GameRuleException.INVALID_ACTION, "Accion desconocida.");
		}

		broadcaster.broadcastState(game);

		if (game.finished()) {
			broadcaster.broadcastOver(game);
			games.remove(room.code());
			room.state(GameState.FINISHED);
		}
	}

	private static NightAction nightAction(GameAction message, String target) {
		String second = emptyToNull(message.secondTargetId());

		return switch (message.kind()) {
			case "seer" -> new SeerAction(target);
			case "guardian" -> new GuardianAction(target);
			case "wolf_vote" -> new WolfVote(target);
			case "witch_heal" -> new WitchAction(WitchPotion.HEAL, target);
			case "witch_poison" -> new WitchAction(WitchPotion.POISON, target);
			case "witch_none" -> new WitchAction(WitchPotion.NONE, null);
			case "cupid" -> new CupidAction(target, second);
			default -> throw new GameRuleException(GameRuleException.INVALID_ACTION,
					"Accion de noche desconocida.");
		};
	}

	private static String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
