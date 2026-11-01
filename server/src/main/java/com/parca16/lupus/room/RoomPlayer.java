package com.parca16.lupus.room;

/** Jugador dentro de una sala, con su estado de conexion. */
public record RoomPlayer(String id, String nickname, boolean host, boolean connected) {

	public RoomPlayer disconnected() {
		return new RoomPlayer(id, nickname, host, false);
	}
}
