package com.parca16.lupus.protocol;

/** El cliente pide volver a la configuracion con los mismos jugadores. */
public record ReturnToLobby(int v, String id, long at) implements Message {

	public ReturnToLobby {
		Protocol.requireVersion(v);
	}
}
