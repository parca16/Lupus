package com.parca16.lupus.protocol;

/** El cliente se presenta tras abrir la conexión. */
public record Hello(int v, String id, long at, String nickname) implements Message {

	public Hello {
		Protocol.requireVersion(v);
	}
}
