package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class DebateTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
	private static final List<String> VIVOS = List.of("p1", "p2", "p3", "p4", "p5", "p6");

	@Test
	void elDebateDuraCuatroMinutosYSePuedeCerrarAntes() {
		Game game = enDebate(1);

		assertEquals(T0.plusSeconds(240), game.deadline());

		game.tick(T0.plusSeconds(239));
		assertEquals(Phase.DEBATE, game.phase());

		game.tick(T0.plusSeconds(240));
		assertEquals(Phase.VOTING, game.phase());
	}

	@Test
	void soloSePuedeMarcarListoDuranteElDebate() {
		Game game = noche(1);

		assertEquals(GameRuleException.NOT_YOUR_TURN,
				assertThrows(GameRuleException.class, () -> game.markReadyToVote("p1")).code());
	}

	@Test
	void cuandoTodosLosVivosEstanListosSePasaAVotar() {
		Game game = enDebate(1);

		for (String vivo : VIVOS.subList(0, VIVOS.size() - 1)) {
			assertFalse(game.markReadyToVote(vivo), "Aun queda gente por marcar");
		}

		assertTrue(game.markReadyToVote("p6"));
		assertEquals(Phase.VOTING, game.phase());
		assertEquals(T0.plusSeconds(45), game.deadline());
	}

	@Test
	void marcarDosVecesEsIdempotente() {
		Game game = enDebate(1);

		game.markReadyToVote("p1");
		game.markReadyToVote("p1");

		assertTrue(game.isReadyToVote("p1"));
		assertEquals(Phase.DEBATE, game.phase());
	}

	@Test
	void unMuertoNoPuedeMarcarListo() {
		Game game = enDebate(1);
		game.kill("p6");

		assertEquals(GameRuleException.NOT_ALIVE,
				assertThrows(GameRuleException.class, () -> game.markReadyToVote("p6")).code());
	}

	@Test
	void elEstadoListoSeReiniciaEnCadaDebate() {
		Game game = enDebate(1);
		game.markReadyToVote("p1");

		game.advance(T0); // DEBATE -> VOTING
		game.advance(T0); // VOTING -> NIGHT
		game.advance(T0); // SEER -> GUARDIAN
		game.advance(T0); // GUARDIAN -> WOLVES
		game.advance(T0); // WOLVES -> WITCH
		game.advance(T0); // WITCH -> DAWN
		game.advance(T0); // DAWN -> DEBATE

		assertEquals(Phase.DEBATE, game.phase());
		assertFalse(game.isReadyToVote("p1"));
	}

	private static Game enDebate(int lives) {
		Game game = noche(lives);
		game.advance(T0); // SEER -> GUARDIAN
		game.advance(T0); // GUARDIAN -> WOLVES
		game.advance(T0); // WOLVES -> WITCH
		game.advance(T0); // WITCH -> DAWN
		game.advance(T0); // DAWN -> DEBATE
		return game;
	}

	private static Game noche(int lives) {
		Game game = game(lives);
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (SEER)
		return game;
	}

	private static Game game(int lives) {
		GameSettings settings = new GameSettings(2, List.of("seer", "guardian", "witch"), lives, false, 240, 20, 45,
				true);
		Map<String, Role> roles = Map.of(
				"p1", Role.WOLF,
				"p2", Role.WOLF,
				"p3", Role.SEER,
				"p4", Role.GUARDIAN,
				"p5", Role.WITCH,
				"p6", Role.VILLAGER);
		return Game.startWithRoles("ABC234", settings, participants(6), roles, CLOCK, new Random(7));
	}

	private static List<GameParticipant> participants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
