package com.parca16.lupus.protocol;

/** La sala se ha cerrado y el jugador deja de pertenecer a ella. */
public record RoomClosed(int v, String id, long at, String roomCode, String reason) implements Message {

	public RoomClosed {
		Protocol.requireVersion(v);
	}
}
