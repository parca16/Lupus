package com.parca16.lupus.game;

/** Accion nocturna enviada por un jugador. */
public sealed interface NightAction permits SeerAction, GuardianAction, WitchAction, WolfVote {

	/** Objetivo de la accion; puede ser nulo en una accion sin objetivo (pocion NONE). */
	String targetId();
}
