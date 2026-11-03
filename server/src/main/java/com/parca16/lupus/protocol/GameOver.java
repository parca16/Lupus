package com.parca16.lupus.protocol;

import java.util.List;

/** Fin de la partida con el bando ganador y todos los roles revelados. */
public record GameOver(int v, String id, long at, String roomCode, String outcome, List<GameRole> roles)
		implements Message {

	public GameOver {
		Protocol.requireVersion(v);
		roles = List.copyOf(roles);
	}
}
