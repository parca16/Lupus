package com.parca16.lupus.protocol;

import java.util.List;

/** Parte del amanecer: muertes, vidas perdidas y curaciones de la noche. */
public record GameDawn(int v, String id, long at, String roomCode, List<String> died, List<String> lostLife,
		List<String> healed, boolean quiet) implements Message {

	public GameDawn {
		Protocol.requireVersion(v);
		died = List.copyOf(died);
		lostLife = List.copyOf(lostLife);
		healed = List.copyOf(healed);
	}
}
