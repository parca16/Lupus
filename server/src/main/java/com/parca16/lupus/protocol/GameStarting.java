package com.parca16.lupus.protocol;

/** La partida empieza: el lobby deja paso al juego. */
public record GameStarting(int v, String id, long at, String roomCode) implements Message {

	public GameStarting {
		Protocol.requireVersion(v);
	}
}
