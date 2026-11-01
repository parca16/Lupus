package com.parca16.lupus.protocol;

import java.util.List;

/** Configuracion vigente de la sala; se manda al crear, al entrar y al cambiarla. */
public record RoomSettings(int v, String id, long at, String roomCode, int wolves, List<String> roles,
		int lives, boolean president, int debateSeconds, int roleSeconds, int voteSeconds,
		boolean showVoteDetail) implements Message {

	public RoomSettings {
		Protocol.requireVersion(v);
		roles = List.copyOf(roles);
	}
}
