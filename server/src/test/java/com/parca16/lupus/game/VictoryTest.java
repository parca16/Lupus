package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class VictoryTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void sinLobosGanaElPueblo() {
		Game game = juego(1, false, Map.of(
				"p1", Role.WOLF,
				"p2", Role.VILLAGER,
				"p3", Role.VILLAGER,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER));

		game.kill("p1");

		assertEquals(Outcome.VILLAGE, game.checkVictory());
		assertEquals(Phase.FINISHED, game.phase());
		assertNull(game.deadline());
	}

	@Test
	void laParidadSinPresidenteDaLaVictoriaALosLobos() {
		Game game = juego(1, false, Map.of(
				"p1", Role.WOLF,
				"p2", Role.WOLF,
				"p3", Role.VILLAGER,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER));

		game.kill("p5");

		assertEquals(Outcome.WOLVES, game.checkVictory());
	}

	@Test
	void conUnPresidenteAldeanoLaParidadNoBasta() {
		Game game = juego(2, true, Map.of(
				"p1", Role.WOLF,
				"p2", Role.WOLF,
				"p3", Role.VILLAGER,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER));
		game.electPresident("p3");

		game.kill("p5");
		assertNull(game.checkVictory(), "2 lobos contra 2 aldeanos con Presidente aldeano: 2 < 3");

		game.kill("p3");
		assertEquals(Outcome.WOLVES, game.checkVictory(), "Sin Presidente, 2 lobos contra 1 aldeano");
	}

	@Test
	void unPresidenteLoboAceleraLaVictoria() {
		Game game = juego(1, true, Map.of(
				"p1", Role.WOLF,
				"p2", Role.VILLAGER,
				"p3", Role.VILLAGER,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER));
		game.electPresident("p1");

		game.kill("p2");
		game.kill("p3");

		assertEquals(Outcome.WOLVES, game.checkVictory(),
				"Lobo + Presidente lobo = 2 votos contra 2 aldeanos");
	}

	@Test
	void siMuerenTodosGananLosLobos() {
		Game game = juego(1, false, Map.of(
				"p1", Role.WOLF,
				"p2", Role.VILLAGER,
				"p3", Role.VILLAGER,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER));

		for (int i = 1; i <= 5; i++) {
			game.kill("p" + i);
		}

		assertEquals(Outcome.WOLVES, game.checkVictory());
	}

	@Test
	void lincharAlUltimoLoboDaLaVictoriaAlPueblo() {
		Game game = juegoConVidasCero();

		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (WOLVES)
		game.advance(T0); // NIGHT -> DAWN
		game.advance(T0); // DAWN -> DEBATE
		game.advance(T0); // DEBATE -> VOTING
		assertEquals(Phase.VOTING, game.phase());

		game.submitVote("p1", "p2");
		game.submitVote("p2", "p1");
		game.submitVote("p3", "p1");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");

		assertTrue(game.lastLynch().died());
		assertEquals(Role.WOLF, game.lastLynch().revealedRole(), "Al morir linchado se revela el rol");
		assertEquals(Outcome.VILLAGE, game.outcome());
		assertEquals(Phase.FINISHED, game.phase());
	}

	private static Game juego(int wolves, boolean president, Map<String, Role> roles) {
		GameSettings settings = new GameSettings(wolves, List.of(), 1, president, 240, 20, 45, true);
		return Game.startWithRoles("ABC234", settings, participants(roles.size()), roles, CLOCK, new Random(7));
	}

	private static Game juegoConVidasCero() {
		GameSettings settings = new GameSettings(1, List.of(), 0, false, 240, 20, 45, true);
		Map<String, Role> roles = Map.of(
				"p1", Role.WOLF,
				"p2", Role.VILLAGER,
				"p3", Role.VILLAGER,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER);
		return Game.startWithRoles("ABC234", settings, participants(5), roles, CLOCK, new Random(7));
	}

	private static List<GameParticipant> participants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
