package com.parca16.lupus.protocol;

import java.util.List;

/** El host cambia la configuracion de la partida. */
public record UpdateSettings(int v, String id, long at, int wolves, List<String> roles, int lives,
		boolean president, int debateSeconds, int roleSeconds, int voteSeconds, boolean showVoteDetail)
		implements Message {

	public UpdateSettings {
		Protocol.requireVersion(v);
		roles = List.copyOf(roles);
	}
}
