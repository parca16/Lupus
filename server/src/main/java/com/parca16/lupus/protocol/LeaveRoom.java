package com.parca16.lupus.protocol;

/** Salida voluntaria de la sala. */
public record LeaveRoom(int v, String id, long at) implements Message {

	public LeaveRoom {
		Protocol.requireVersion(v);
	}
}
