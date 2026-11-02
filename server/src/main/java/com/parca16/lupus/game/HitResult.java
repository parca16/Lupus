package com.parca16.lupus.game;

/** Resultado de aplicar un golpe a un jugador. */
public enum HitResult {
	/** Perdio una vida pero sigue vivo. */
	LOST_LIFE,
	/** Llego a cero vidas y muere de forma definitiva. */
	DIED,
	/** Ya estaba muerto. */
	ALREADY_DEAD
}
