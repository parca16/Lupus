package com.parca16.lupus.protocol;

/** Recupera el asiento de un jugador con su identidad persistente. */
public record ResumeRoom(int v, String id, long at, String roomCode, String playerId) implements Message {

	public ResumeRoom {
		Protocol.requireVersion(v);
	}
}
