package com.parca16.lupus.protocol;

/** Mensaje de chat enviado por un jugador. */
public record ChatSend(int v, String id, long at, String channel, String text) implements Message {

	public ChatSend {
		Protocol.requireVersion(v);
	}
}
