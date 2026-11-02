package com.parca16.lupus.room;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Registro de salas en memoria. */
@Component
public class RoomRegistry {

	static final int CODE_LENGTH = 6;
	private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final int MAX_ATTEMPTS = 100;

	private final Map<String, Room> rooms = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();
	private final Clock clock;

	public RoomRegistry(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	/** Crea una sala nueva con codigo unico. */
	public Room create() {
		return create(RoomCapacity.DEFAULT);
	}

	public Room create(RoomCapacity capacity) {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String code = generateCode();
			Room room = new Room(code, capacity, clock);
			if (rooms.putIfAbsent(code, room) == null) {
				return room;
			}
		}
		throw new IllegalStateException("No se pudo generar un codigo de sala unico");
	}

	public Optional<Room> find(String code) {
		if (code == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(rooms.get(normalize(code)));
	}

	public void remove(String code) {
		if (code != null) {
			rooms.remove(normalize(code));
		}
	}

	/**
	 * Saca del registro las salas sin actividad durante {@code ttl} y las devuelve.
	 *
	 * <p>El borrado es atomico con la comprobacion: si una sala recibe actividad entre medias,
	 * no se elimina.
	 */
	public List<Room> removeExpired(Instant now, Duration ttl) {
		List<Room> removed = new ArrayList<>();

		for (Room room : rooms.values()) {
			if (room.expired(now, ttl) && rooms.remove(room.code(), room)) {
				removed.add(room);
			}
		}

		return removed;
	}

	public int size() {
		return rooms.size();
	}

	private String generateCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
		}
		return code.toString();
	}

	static String normalize(String code) {
		return code.strip().toUpperCase(Locale.ROOT);
	}
}
