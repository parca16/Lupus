package com.parca16.lupus.ws;

import static com.parca16.lupus.ws.SessionAttributes.CONNECTION_ID;
import static com.parca16.lupus.ws.SessionAttributes.NICKNAME;
import static com.parca16.lupus.ws.SessionAttributes.PLAYER_ID;
import static com.parca16.lupus.ws.SessionAttributes.ROOM_CODE;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.parca16.lupus.protocol.CreateRoom;
import com.parca16.lupus.protocol.Echo;
import com.parca16.lupus.protocol.ErrorMessage;
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

	public LupusWebSocketHandler(RoomRegistry rooms, PlayerSessions sessions, RoomNotifier notifier) {
		this.rooms = rooms;
		this.sessions = sessions;
		this.notifier = notifier;
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

		int conectados = (int) room.players().stream().filter(RoomPlayer::connected).count();
		room.gameSettings().validatedForStart(conectados);
		room.state(GameState.STARTING);

		notifier.broadcastAll(room, new GameStarting(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), room.code()));
	}

	private void salirDeSala(WebSocketSession session) {
		Room room = salaActual(session);
		String playerId = jugadorActual(session);

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
}
