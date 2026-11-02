package com.parca16.lupus.protocol;

/** Fin de la partida con el bando ganador. */
public record GameOver(int v, String id, long at, String roomCode, String outcome) implements Message {

	public GameOver {
		Protocol.requireVersion(v);
	}
}
