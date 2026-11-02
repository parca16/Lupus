package com.parca16.lupus.game;

/** Suceso anunciado al amanecer. Nunca incluye el rol: las muertes nocturnas no lo revelan (D7). */
public record DawnEvent(Type type, String playerId, String nickname, int livesLeft) {

	public enum Type {
		DIED,
		LOST_LIFE,
		HEALED
	}
}
