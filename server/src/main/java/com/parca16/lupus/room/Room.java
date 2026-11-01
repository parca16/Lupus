package com.parca16.lupus.room;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Sala privada: codigo, jugadores, estado y configuracion.
 *
 * <p>Todos los metodos que tocan el estado estan sincronizados porque las peticiones pueden
 * llegar por varias conexiones WebSocket a la vez.
 */
public final class Room {

	private final String code;
	private final RoomSettings settings;
	private final Map<String, RoomPlayer> players = new LinkedHashMap<>();
	private GameState state = GameState.WAITING;

	Room(String code, RoomSettings settings) {
		this.code = Objects.requireNonNull(code, "code");
		this.settings = Objects.requireNonNull(settings, "settings");
	}

	public String code() {
		return code;
	}

	public RoomSettings settings() {
		return settings;
	}

	public synchronized GameState state() {
		return state;
	}

	public synchronized void state(GameState state) {
		this.state = Objects.requireNonNull(state, "state");
	}

	/** Anade un jugador; el primero de la sala es el host. */
	public synchronized RoomPlayer addPlayer(String nickname) {
		String clean = Nicknames.require(nickname);

		if (players.size() >= settings.maxPlayers()) {
			throw new RoomException(RoomException.ROOM_FULL,
					"La sala esta completa (" + settings.maxPlayers() + " jugadores).");
		}

		boolean taken = players.values().stream()
				.anyMatch(player -> player.nickname().equalsIgnoreCase(clean));
		if (taken) {
			throw new RoomException(RoomException.NICKNAME_TAKEN, "Ese apodo ya esta en uso en la sala.");
		}

		RoomPlayer player = new RoomPlayer(UUID.randomUUID().toString(), clean, players.isEmpty(), true);
		players.put(player.id(), player);
		return player;
	}

	/** Marca a un jugador como desconectado sin sacarlo de la sala. */
	public synchronized RoomPlayer disconnect(String playerId) {
		RoomPlayer player = players.get(playerId);
		if (player == null) {
			throw new RoomException(RoomException.NOT_IN_ROOM, "El jugador no pertenece a esta sala.");
		}

		RoomPlayer disconnected = player.disconnected();
		players.put(playerId, disconnected);
		return disconnected;
	}

	public synchronized List<RoomPlayer> players() {
		return List.copyOf(players.values());
	}

	public synchronized boolean contains(String playerId) {
		return players.containsKey(playerId);
	}

	public synchronized int size() {
		return players.size();
	}
}
