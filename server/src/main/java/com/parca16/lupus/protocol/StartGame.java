package com.parca16.lupus.protocol;

/** El host arranca la partida. */
public record StartGame(int v, String id, long at) implements Message {

	public StartGame {
		Protocol.requireVersion(v);
	}
}
