package com.parca16.lupus.protocol;

/** El servidor rechaza un mensaje y explica por qué. */
public record ErrorMessage(int v, String id, long at, String code, String message) implements Message {

	public ErrorMessage {
		Protocol.requireVersion(v);
	}
}
