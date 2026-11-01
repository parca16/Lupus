package com.parca16.lupus.protocol;

/** El host ha expulsado a este jugador de la sala. */
public record Kicked(int v, String id, long at, String roomCode) implements Message {

	public Kicked {
		Protocol.requireVersion(v);
	}
}
