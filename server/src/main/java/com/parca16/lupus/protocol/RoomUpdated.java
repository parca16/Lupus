package com.parca16.lupus.protocol;

import java.util.List;

/** La lista de jugadores de la sala ha cambiado. */
public record RoomUpdated(int v, String id, long at, String roomCode, List<Player> players) implements Message {

	public RoomUpdated {
		Protocol.requireVersion(v);
		players = List.copyOf(players);
	}
}
