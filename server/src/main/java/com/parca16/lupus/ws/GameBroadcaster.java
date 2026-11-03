package com.parca16.lupus.ws;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parca16.lupus.game.DawnEvent;
import com.parca16.lupus.game.DawnReport;
import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GamePlayer;
import com.parca16.lupus.game.GameView;
import com.parca16.lupus.game.Vote;
import com.parca16.lupus.game.VoteResult;
import com.parca16.lupus.protocol.GameDawn;
import com.parca16.lupus.protocol.GameOver;
import com.parca16.lupus.protocol.GameRole;
import com.parca16.lupus.protocol.GameState;
import com.parca16.lupus.protocol.GameStatus;
import com.parca16.lupus.protocol.GameVoteCount;
import com.parca16.lupus.protocol.Protocol;

/** Construye y envia los mensajes personalizados de la partida. */
@Component
public class GameBroadcaster {

	private final PlayerSessions sessions;
	private final RoomNotifier notifier;

	public GameBroadcaster(PlayerSessions sessions, RoomNotifier notifier) {
		this.sessions = sessions;
		this.notifier = notifier;
	}

	public void sendState(Game game, String playerId) {
		sessions.find(playerId).ifPresent(session -> notifier.send(session, stateFor(game, playerId)));
	}

	public void broadcastState(Game game) {
		for (GamePlayer player : game.players()) {
			sendState(game, player.id());
		}
	}

	public void broadcastDawn(Game game, DawnReport report) {
		GameDawn message = new GameDawn(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), game.roomCode(),
				ids(report, DawnEvent.Type.DIED),
				ids(report, DawnEvent.Type.LOST_LIFE),
				ids(report, DawnEvent.Type.HEALED),
				report.quiet());

		for (GamePlayer player : game.players()) {
			sessions.find(player.id()).ifPresent(session -> notifier.send(session, message));
		}
	}

	public void broadcastOver(Game game) {
		GameOver message = overFor(game);

		for (GamePlayer player : game.players()) {
			sessions.find(player.id()).ifPresent(session -> notifier.send(session, message));
		}
	}

	/** Mensaje final con el bando ganador y todos los roles revelados. */
	GameOver overFor(Game game) {
		List<GameRole> roles = game.players().stream()
				.map(player -> new GameRole(player.id(), player.nickname(),
						player.role() == null ? "" : player.role().name()))
				.toList();

		String outcome = game.outcome() == null ? "" : game.outcome().name();
		return new GameOver(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				game.roomCode(), outcome, roles);
	}

	private static List<String> ids(DawnReport report, DawnEvent.Type type) {
		return report.events().stream()
				.filter(event -> event.type() == type)
				.map(DawnEvent::playerId)
				.toList();
	}

	GameState stateFor(Game game, String playerId) {
		GameView view = game.viewFor(playerId);

		List<GameStatus> statuses = game.players().stream()
				.map(player -> new GameStatus(player.id(), player.nickname(), player.lives(),
						player.alive(), player.president()))
				.toList();

		List<String> ready = game.players().stream()
				.filter(player -> game.isReadyToVote(player.id()))
				.map(GamePlayer::id)
				.toList();

		Vote myVote = game.voteOf(playerId).orElse(null);
		VoteResult lastVote = game.lastVoteResult();
		List<GameVoteCount> lastVoteCounts = lastVote == null ? List.of()
				: lastVote.counts().entrySet().stream()
						.map(entry -> new GameVoteCount(entry.getKey(), entry.getValue()))
						.toList();

		return new GameState(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				game.roomCode(),
				game.phase().name(),
				game.round(),
				game.deadline() == null ? 0 : game.deadline().toEpochMilli(),
				game.nightStep() == null ? "" : game.nightStep().name(),
				view.role() == null ? "" : view.role().name(),
				game.loverOf(playerId).orElse(""),
				game.seerVision(playerId).map(Enum::name).orElse(""),
				game.wolfTargetFor(playerId).orElse(""),
				game.pendingKnightId() == null ? "" : game.pendingKnightId(),
				myVote == null || myVote.targetId() == null ? "" : myVote.targetId(),
				myVote != null,
				game.runoffCandidates(),
				lastVote == null ? 0 : game.lastVoteRound(),
				lastVote != null && lastVote.runoffNeeded(),
				lastVote == null || lastVote.lynchedId() == null ? "" : lastVote.lynchedId(),
				lastVote == null ? List.of() : lastVote.tied(),
				lastVote == null ? 0 : lastVote.abstentions(),
				lastVoteCounts,
				ready,
				statuses);
	}
}
