package com.parca16.lupus.protocol;

/** El host expulsa a un jugador de la sala. */
public record KickPlayer(int v, String id, long at, String playerId) implements Message {

	public KickPlayer {
		Protocol.requireVersion(v);
	}
}
