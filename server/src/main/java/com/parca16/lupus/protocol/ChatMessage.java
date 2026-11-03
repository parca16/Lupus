package com.parca16.lupus.protocol;

/** Mensaje de chat entregado a los jugadores de un canal. */
public record ChatMessage(int v, String id, long at, String roomCode, String channel, String senderId,
		String senderNickname, String text, int round) implements Message {

	public ChatMessage {
		Protocol.requireVersion(v);
	}
}
