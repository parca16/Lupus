package com.parca16.lupus.ws;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GameRegistry;
import com.parca16.lupus.game.Phase;
import com.parca16.lupus.room.GameState;
import com.parca16.lupus.room.RoomRegistry;

/** Avanza las partidas cuando vencen sus plazos y difunde los cambios (issue #34). */
@Component
public class GameTicker {

	private final GameRegistry games;
	private final GameBroadcaster broadcaster;
	private final RoomRegistry rooms;
	private final Clock clock;
	private final Set<String> dawnSent = new HashSet<>();

	public GameTicker(GameRegistry games, GameBroadcaster broadcaster, RoomRegistry rooms, Clock clock) {
		this.games = games;
		this.broadcaster = broadcaster;
		this.rooms = rooms;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${lupus.game.tick-millis:1000}")
	public void tick() {
		tick(clock.instant());
	}

	/** Avance manual, usado por tests y por el planificador. */
	public void tick(Instant now) {
		for (Game game : games.all()) {
			Phase before = game.phase();
			String roomCode = game.roomCode();
			String dawnKey = roomCode + "#" + game.round();

			game.tick(now);

			if (game.finished()) {
				broadcaster.broadcastOver(game);
				rooms.find(roomCode).ifPresent(room -> room.state(GameState.FINISHED));
				games.remove(roomCode);
				dawnSent.removeIf(key -> key.startsWith(roomCode + "#"));
				continue;
			}

			if (game.dawnReport() != null && dawnSent.add(dawnKey)) {
				broadcaster.broadcastDawn(game, game.dawnReport());
			}

			if (game.phase() != before) {
				broadcaster.broadcastState(game);
			}
		}
	}
}
