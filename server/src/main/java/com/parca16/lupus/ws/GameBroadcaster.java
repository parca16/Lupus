package com.parca16.lupus.ws;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parca16.lupus.game.DawnEvent;
import com.parca16.lupus.game.DawnReport;
import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GamePlayer;
import com.parca16.lupus.game.GameView;
import com.parca16.lupus.protocol.GameDawn;
import com.parca16.lupus.protocol.GameOver;
import com.parca16.lupus.protocol.GameState;
import com.parca16.lupus.protocol.GameStatus;
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
		String outcome = game.outcome() == null ? "" : game.outcome().name();
		GameOver message = new GameOver(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), game.roomCode(), outcome);

		for (GamePlayer player : game.players()) {
			sessions.find(player.id()).ifPresent(session -> notifier.send(session, message));
		}
	}

	private static List<String> ids(DawnReport report, DawnEvent.Type type) {
		return report.events().stream()
				.filter(event -> event.type() == type)
				.map(DawnEvent::playerId)
				.toList();
	}

	private GameState stateFor(Game game, String playerId) {
		GameView view = game.viewFor(playerId);

		List<GameStatus> statuses = game.players().stream()
				.map(player -> new GameStatus(player.id(), player.nickname(), player.lives(),
						player.alive(), player.president()))
				.toList();

		List<String> ready = game.players().stream()
				.filter(player -> game.isReadyToVote(player.id()))
				.map(GamePlayer::id)
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
				ready,
				statuses);
	}
}
