package com.parca16.lupus.protocol;

/** Handshake del servidor: identifica la conexión y confirma el apodo recibido. */
public record Welcome(int v, String id, long at, String connectionId, String nickname, long serverTime)
		implements Message {

	public Welcome {
		Protocol.requireVersion(v);
	}
}
