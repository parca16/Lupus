package com.parca16.lupus.protocol;

/** La sala vuelve al lobby tras terminar la partida. */
public record ReturnedToLobby(int v, String id, long at, String roomCode) implements Message {

	public ReturnedToLobby {
		Protocol.requireVersion(v);
	}
}
