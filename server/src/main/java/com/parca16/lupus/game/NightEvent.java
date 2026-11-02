package com.parca16.lupus.game;

/** Suceso de la noche, con su efecto ya resuelto. */
public record NightEvent(Type type, String targetId, boolean blocked, boolean applied, HitResult result) {

	public enum Type {
		WOLF_ATTACK,
		POISON,
		HEAL
	}
}
