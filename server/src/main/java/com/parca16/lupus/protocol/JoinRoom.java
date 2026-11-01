package com.parca16.lupus.protocol;

/** Entra en una sala existente con su codigo. */
public record JoinRoom(int v, String id, long at, String roomCode, String nickname) implements Message {

	public JoinRoom {
		Protocol.requireVersion(v);
	}
}
