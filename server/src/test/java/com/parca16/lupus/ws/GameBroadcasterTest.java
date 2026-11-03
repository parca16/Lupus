package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GameParticipant;
import com.parca16.lupus.game.Outcome;
import com.parca16.lupus.game.Phase;
import com.parca16.lupus.protocol.GameOver;
import com.parca16.lupus.protocol.GameState;
import com.parca16.lupus.room.GameSettings;
import com.parca16.lupus.room.RoomRegistry;

/** El estado personalizado no filtra votos ajenos y publica el recuento al cerrar (#59). */
class GameBroadcasterTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	private final PlayerSessions sessions = new PlayerSessions();
	private final GameBroadcaster broadcaster = new GameBroadcaster(sessions,
			new RoomNotifier(sessions, new RoomRegistry(CLOCK)));

	@Test
	void cadaJugadorSoloVeSuPropioVoto() {
		Game game = enVotacion();

		game.submitVote("p1", "p6");

		GameState paraP1 = broadcaster.stateFor(game, "p1");
		GameState paraP2 = broadcaster.stateFor(game, "p2");

		assertEquals("p6", paraP1.myVote());
		assertTrue(paraP1.hasVoted());
		assertEquals("", paraP2.myVote());
		assertFalse(paraP2.hasVoted());
		assertEquals("", paraP2.lastVoteLynchedId(), "Todavia no hay recuento");
	}

	@Test
	void alCerrarPublicaRecuentoYLinchado() {
		Game game = enVotacion();

		votar(game);

		GameState estado = broadcaster.stateFor(game, "p1");

		assertEquals(1, estado.lastVoteRound());
		assertEquals("p6", estado.lastVoteLynchedId());
		assertFalse(estado.lastVoteRunoffNeeded());
		assertEquals(0, estado.lastVoteAbstentions());
		assertEquals(5, votosDe(estado, "p6"));
		assertEquals(1, votosDe(estado, "p1"));
	}

	@Test
	void elEmpateExponeLosCandidatosDeLaSegundaVuelta() {
		Game game = enVotacion();

		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p6");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");
		game.submitVote("p6", "p1");

		GameState estado = broadcaster.stateFor(game, "p2");

		assertEquals(Phase.RUNOFF.name(), estado.phase());
		assertTrue(estado.lastVoteRunoffNeeded());
		assertEquals("", estado.lastVoteLynchedId());
		assertEquals(List.of("p6", "p1"), estado.runoffCandidates());
		assertEquals(2, estado.lastVoteTied().size());
		assertEquals("", estado.myVote(), "La segunda vuelta empieza sin votos");
		assertFalse(estado.hasVoted());
	}

	@Test
	void elFinalRevelaElBandoYLosRolesDeTodos() {
		Game game = enVotacion();
		game.finish(Outcome.VILLAGE);

		GameOver fin = broadcaster.overFor(game);

		assertEquals("VILLAGE", fin.outcome());
		assertEquals(6, fin.roles().size());
		assertEquals("Jugador1", fin.roles().get(0).nickname());
		assertTrue(fin.roles().stream().allMatch(role -> !role.role().isEmpty()));
		assertTrue(fin.roles().stream().anyMatch(role -> role.role().equals("WOLF")));
	}

	private static int votosDe(GameState estado, String playerId) {
		return estado.lastVoteCounts().stream()
				.filter(count -> count.playerId().equals(playerId))
				.mapToInt(count -> count.votes())
				.findFirst()
				.orElse(0);
	}

	private static void votar(Game game) {
		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p6");
		game.submitVote("p4", "p6");
		game.submitVote("p5", "p6");
		game.submitVote("p6", "p1");
	}

	private static Game enVotacion() {
		GameSettings settings = new GameSettings(1, List.of(), 1, false, 240, 20, 45, true);
		Game game = Game.start("ABC234", settings, participantes(), CLOCK, new Random(7));

		for (int i = 0; i < 10 && game.phase() != Phase.VOTING; i++) {
			game.advance(T0);
		}
		assertEquals(Phase.VOTING, game.phase());
		return game;
	}

	private static List<GameParticipant> participantes() {
		return IntStream.rangeClosed(1, 6)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
