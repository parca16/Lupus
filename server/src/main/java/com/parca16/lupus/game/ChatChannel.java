package com.parca16.lupus.game;

import java.util.Optional;

/** Canales de chat del juego. */
public enum ChatChannel {
	PUBLIC,
	WOLVES,
	LOVERS;

	/** Traduce el identificador que viaja por el cable. */
	public static Optional<ChatChannel> fromWire(String value) {
		if (value == null) {
			return Optional.empty();
		}
		return switch (value) {
			case "public" -> Optional.of(PUBLIC);
			case "wolves" -> Optional.of(WOLVES);
			case "lovers" -> Optional.of(LOVERS);
			default -> Optional.empty();
		};
	}

	public String wire() {
		return name().toLowerCase();
	}
}
