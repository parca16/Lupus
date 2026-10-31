package com.parca16.lupus.protocol;

/** Respuesta a {@link Ping}. */
public record Pong(int v, String id, long at) implements Message {

	public Pong {
		Protocol.requireVersion(v);
	}
}
