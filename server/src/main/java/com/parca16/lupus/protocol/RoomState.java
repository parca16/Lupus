package com.parca16.lupus.protocol;

import java.util.List;

/** Respuesta a create_room y join_room: la sala y quien eres en ella. */
public record RoomState(int v, String id, long at, String roomCode, String playerId, List<Player> players)
		implements Message {

	public RoomState {
		Protocol.requireVersion(v);
		players = List.copyOf(players);
	}
}
