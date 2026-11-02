package com.parca16.lupus.ws;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Player;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.protocol.RoomClosed;
import com.parca16.lupus.protocol.RoomUpdated;
import com.parca16.lupus.room.Room;
import com.parca16.lupus.room.RoomPlayer;
import com.parca16.lupus.room.RoomRegistry;

/** Envio de mensajes a sesiones y cierre de salas, compartido por el handler y la limpieza. */
@Component
public class RoomNotifier {

	public static final String HOST_LEFT = "host_left";
	public static final String INACTIVITY = "inactivity";

	private final PlayerSessions sessions;
	private final RoomRegistry rooms;

	public RoomNotifier(PlayerSessions sessions, RoomRegistry rooms) {
		this.sessions = sessions;
		this.rooms = rooms;
	}

	public void send(WebSocketSession session, Message message) {
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

	/** Difunde la lista de jugadores a todos menos al excluido. */
	public void broadcastPlayers(Room room, String excludedPlayerId) {
		List<Player> players = wire(room.players());

		for (RoomPlayer player : room.players()) {
			if (player.id().equals(excludedPlayerId)) {
				continue;
			}
			sessions.find(player.id()).ifPresent(target -> send(target, new RoomUpdated(Protocol.VERSION,
					UUID.randomUUID().toString(), System.currentTimeMillis(), room.code(), players)));
		}
	}

	/** Manda el mismo mensaje a todos los jugadores de la sala. */
	public void broadcastAll(Room room, Message message) {
		for (RoomPlayer player : room.players()) {
			sessions.find(player.id()).ifPresent(target -> send(target, message));
		}
	}

	/**
	 * Cierra la sala: la saca del registro y avisa a todos menos al excluido.
	 *
	 * <p>El orden importa: en cuanto un jugador recibe {@code room_closed}, la sala ya no debe
	 * existir, para que ningun {@code join_room} concurrente pueda colarse en ella.
	 */
	public void close(Room room, String reason, String excludedPlayerId) {
		rooms.remove(room.code());

		for (RoomPlayer player : room.players()) {
			if (player.id().equals(excludedPlayerId)) {
				continue;
			}
			sessions.find(player.id()).ifPresent(target -> {
				send(target, new RoomClosed(Protocol.VERSION, UUID.randomUUID().toString(),
						System.currentTimeMillis(), room.code(), reason));
				SessionAttributes.clearMembership(target);
				sessions.unbind(player.id());
			});
		}

		rooms.remove(room.code());
	}

	static List<Player> wire(List<RoomPlayer> players) {
		return players.stream()
				.map(player -> new Player(player.id(), player.nickname(), player.host(), player.connected()))
				.toList();
	}
}
