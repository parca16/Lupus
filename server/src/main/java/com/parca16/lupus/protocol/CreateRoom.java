package com.parca16.lupus.protocol;

/** Crea una sala privada; el creador pasa a ser host. */
public record CreateRoom(int v, String id, long at, String nickname) implements Message {

	public CreateRoom {
		Protocol.requireVersion(v);
	}
}
