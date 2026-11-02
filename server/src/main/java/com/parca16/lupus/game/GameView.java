package com.parca16.lupus.game;

import java.time.Instant;
import java.util.List;

/**
 * Vista de la partida para un jugador concreto.
 *
 * <p>Por diseño no incluye los roles de los demas: el reparto es secreto (issue #35).
 */
public record GameView(String roomCode, Phase phase, int round, NightStep nightStep, Instant deadline,
		String playerId, Role role, int lives, boolean alive, boolean president,
		List<PublicPlayer> players) {

	/** Datos publicos del resto de jugadores. */
	public record PublicPlayer(String id, String nickname, int lives, boolean alive, boolean president) {
	}
}
