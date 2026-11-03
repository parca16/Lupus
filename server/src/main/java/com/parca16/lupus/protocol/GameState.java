package com.parca16.lupus.protocol;

import java.util.List;

/**
 * Estado personalizado de la partida: fase, plazos, tu rol privado y el estado publico.
 *
 * <p>Cada jugador recibe su propia version; {@code role} y {@code vision} nunca son de otros.
 */
public record GameState(int v, String id, long at, String roomCode, String phase, int round, long deadline,
		String nightStep, String role, String loverId, String vision, String witchVictim, String pendingKnight,
		String myVote, boolean hasVoted, List<String> runoffCandidates, int lastVoteRound,
		boolean lastVoteRunoffNeeded, String lastVoteLynchedId, List<String> lastVoteTied,
		int lastVoteAbstentions, List<GameVoteCount> lastVoteCounts, List<String> ready,
		List<GameStatus> statuses) implements Message {

	public GameState {
		Protocol.requireVersion(v);
		runoffCandidates = List.copyOf(runoffCandidates);
		lastVoteTied = List.copyOf(lastVoteTied);
		lastVoteCounts = List.copyOf(lastVoteCounts);
		ready = List.copyOf(ready);
		statuses = List.copyOf(statuses);
	}
}
