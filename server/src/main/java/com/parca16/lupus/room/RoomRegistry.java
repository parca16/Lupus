package com.parca16.lupus.room;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
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

	/** Crea una sala nueva con codigo unico. */
	public Room create() {
		return create(RoomSettings.DEFAULT);
	}

	public Room create(RoomSettings settings) {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String code = generateCode();
			Room room = new Room(code, settings);
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
