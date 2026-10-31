package com.parca16.lupus.protocol;

/** Comprobación de vida del cliente. */
public record Ping(int v, String id, long at) implements Message {

	public Ping {
		Protocol.requireVersion(v);
	}
}
