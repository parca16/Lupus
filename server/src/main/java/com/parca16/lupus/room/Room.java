package com.parca16.lupus.room;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
	private final RoomCapacity capacity;
	private final Clock clock;
	private final Map<String, RoomPlayer> players = new LinkedHashMap<>();
	private GameState state = GameState.WAITING;
	private GameSettings gameSettings = GameSettings.DEFAULT;
	private Instant lastActivityAt;

	Room(String code, RoomCapacity capacity) {
		this(code, capacity, Clock.systemUTC());
	}

	Room(String code, RoomCapacity capacity, Clock clock) {
		this.code = Objects.requireNonNull(code, "code");
		this.capacity = Objects.requireNonNull(capacity, "capacity");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.lastActivityAt = clock.instant();
	}

	public String code() {
		return code;
	}

	public RoomCapacity capacity() {
		return capacity;
	}

	public synchronized GameSettings gameSettings() {
		return gameSettings;
	}

	public synchronized void gameSettings(GameSettings settings) {
		this.gameSettings = Objects.requireNonNull(settings, "settings").validated();
		touch();
	}

	public synchronized GameState state() {
		return state;
	}

	public synchronized void state(GameState state) {
		this.state = Objects.requireNonNull(state, "state");
		touch();
	}

	/** Marca la sala como activa; cualquier mensaje de sus jugadores la mantiene viva. */
	public synchronized void touch() {
		this.lastActivityAt = clock.instant();
	}

	public synchronized Instant lastActivityAt() {
		return lastActivityAt;
	}

	/** Una sala caduca cuando pasan {@code ttl} sin ninguna actividad. */
	public synchronized boolean expired(Instant now, Duration ttl) {
		return !lastActivityAt.plus(ttl).isAfter(now);
	}

	/** Anade un jugador; el primero de la sala es el host. */
	public synchronized RoomPlayer addPlayer(String nickname) {
		String clean = Nicknames.require(nickname);

		if (players.size() >= capacity.maxPlayers()) {
			throw new RoomException(RoomException.ROOM_FULL,
					"La sala esta completa (" + capacity.maxPlayers() + " jugadores).");
		}

		boolean taken = players.values().stream()
				.anyMatch(player -> player.nickname().equalsIgnoreCase(clean));
		if (taken) {
			throw new RoomException(RoomException.NICKNAME_TAKEN, "Ese apodo ya esta en uso en la sala.");
		}

		RoomPlayer player = new RoomPlayer(UUID.randomUUID().toString(), clean, players.isEmpty(), true);
		players.put(player.id(), player);
		touch();
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

	/** Saca a un jugador de la sala. */
	public synchronized RoomPlayer remove(String playerId) {
		RoomPlayer removed = players.remove(playerId);
		if (removed == null) {
			throw new RoomException(RoomException.NOT_IN_ROOM, "El jugador no pertenece a esta sala.");
		}
		touch();
		return removed;
	}

	/**
	 * Recupera el asiento de un jugador desconectado a partir de su identidad persistente.
	 *
	 * <p>El {@code playerId} es el que el navegador guarda en {@code localStorage}; si ese asiento
	 * ya esta conectado (por ejemplo, otra pestaña), se rechaza.
	 */
	public synchronized RoomPlayer reconnect(String playerId) {
		RoomPlayer player = players.get(playerId);

		if (player == null) {
			throw new RoomException(RoomException.SEAT_NOT_FOUND, "Ese asiento ya no existe en la sala.");
		}
		if (player.connected()) {
			throw new RoomException(RoomException.ALREADY_CONNECTED, "Ese jugador ya esta conectado.");
		}

		RoomPlayer reconnected = new RoomPlayer(player.id(), player.nickname(), player.host(), true);
		players.put(playerId, reconnected);
		touch();
		return reconnected;
	}

	public synchronized boolean isHost(String playerId) {
		RoomPlayer player = players.get(playerId);
		return player != null && player.host();
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
