package com.parca16.lupus.ws;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.parca16.lupus.protocol.CreateRoom;
import com.parca16.lupus.protocol.Echo;
import com.parca16.lupus.protocol.ErrorMessage;
import com.parca16.lupus.protocol.Hello;
import com.parca16.lupus.protocol.JoinRoom;
import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Ping;
import com.parca16.lupus.protocol.Player;
import com.parca16.lupus.protocol.Pong;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.protocol.RoomState;
import com.parca16.lupus.protocol.RoomUpdated;
import com.parca16.lupus.protocol.Welcome;
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

	static final String CONNECTION_ID = "connectionId";
	static final String NICKNAME = "nickname";
	static final String PLAYER_ID = "playerId";
	static final String ROOM_CODE = "roomCode";

	private final RoomRegistry rooms;
	private final PlayerSessions sessions;

	public LupusWebSocketHandler(RoomRegistry rooms, PlayerSessions sessions) {
		this.rooms = rooms;
		this.sessions = sessions;
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
				broadcast(room, playerId);
			}
		});
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

		try {
			dispatch(session, decoded);
		}
		catch (RoomException exception) {
			sendError(session, exception.code(), exception.getMessage());
		}
	}

	private void dispatch(WebSocketSession session, Message message) {
		switch (message) {
			case Hello hello -> {
				session.getAttributes().put(NICKNAME, hello.nickname());
				sendWelcome(session);
			}
			case Echo echo -> send(session, new Echo(Protocol.VERSION, echo.id(), echo.at(), echo.text()));
			case Ping ping -> send(session, new Pong(Protocol.VERSION, ping.id(), System.currentTimeMillis()));
			case CreateRoom createRoom -> crearSala(session, createRoom);
			case JoinRoom joinRoom -> unirseASala(session, joinRoom);
			default -> sendError(session, "unknown_type", "Mensaje no admitido del cliente.");
		}
	}

	private void crearSala(WebSocketSession session, CreateRoom message) {
		Room room = rooms.create();
		RoomPlayer player = room.addPlayer(message.nickname());

		vincular(session, room, player);
		sendRoomState(session, room, player);
	}

	private void unirseASala(WebSocketSession session, JoinRoom message) {
		Room room = rooms.find(message.roomCode()).orElseThrow(() -> new RoomException(
				RoomException.ROOM_NOT_FOUND, "No existe ninguna sala con el codigo " + message.roomCode()));

		RoomPlayer player = room.addPlayer(message.nickname());

		vincular(session, room, player);
		sendRoomState(session, room, player);
		broadcast(room, player.id());
	}

	private void vincular(WebSocketSession session, Room room, RoomPlayer player) {
		session.getAttributes().put(PLAYER_ID, player.id());
		session.getAttributes().put(ROOM_CODE, room.code());
		session.getAttributes().put(NICKNAME, player.nickname());
		sessions.bind(player.id(), session);
	}

	private void sendRoomState(WebSocketSession session, Room room, RoomPlayer player) {
		send(session, new RoomState(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				room.code(), player.id(), wire(room.players())));
	}

	private void broadcast(Room room, String excludedPlayerId) {
		List<Player> players = wire(room.players());

		for (RoomPlayer player : room.players()) {
			if (player.id().equals(excludedPlayerId)) {
				continue;
			}
			sessions.find(player.id()).ifPresent(target -> send(target, new RoomUpdated(Protocol.VERSION,
					UUID.randomUUID().toString(), System.currentTimeMillis(), room.code(), players)));
		}
	}

	private static List<Player> wire(List<RoomPlayer> players) {
		return players.stream()
				.map(player -> new Player(player.id(), player.nickname(), player.host(), player.connected()))
				.toList();
	}

	private void sendWelcome(WebSocketSession session) {
		long now = System.currentTimeMillis();
		send(session, new Welcome(Protocol.VERSION, UUID.randomUUID().toString(), now,
				(String) session.getAttributes().get(CONNECTION_ID),
				(String) session.getAttributes().get(NICKNAME), now));
	}

	private void sendError(WebSocketSession session, String code, String message) {
		send(session, new ErrorMessage(Protocol.VERSION,
				UUID.randomUUID().toString(), System.currentTimeMillis(), code, message));
	}

	private void send(WebSocketSession session, Message message) {
		try {
			synchronized (session) {
				if (session.isOpen()) {
					session.sendMessage(new TextMessage(Protocol.encode(message)));
				}
			}
		}
		catch (IOException exception) {
			throw new UncheckedIOException("No se pudo enviar el mensaje", exception);
		}
	}
}
