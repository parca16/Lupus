package com.parca16.lupus.game;

/** Carta de un jugador. */
public enum Role {
	WOLF,
	VILLAGER,
	SEER,
	WITCH,
	KNIGHT,
	GUARDIAN,
	CUPID;

	/** Traduce el identificador que llega en la configuracion de la sala. */
	public static Role fromSettingsId(String id) {
		return switch (id) {
			case "seer" -> SEER;
			case "witch" -> WITCH;
			case "knight" -> KNIGHT;
			case "guardian" -> GUARDIAN;
			case "cupid" -> CUPID;
			default -> throw new IllegalArgumentException("Rol especial desconocido: " + id);
		};
	}

	public boolean isWolf() {
		return this == WOLF;
	}
}
