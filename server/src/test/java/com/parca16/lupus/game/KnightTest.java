package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class KnightTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void elCaballeroMuertoDeNocheEligeAlAmanecer() {
		Game game = caballeroMuertoPorDosGolpes();
		assertEquals(Phase.DAWN, game.phase());

		game.advance(T0); // DAWN -> KNIGHT_REACTION
		assertEquals(Phase.KNIGHT_REACTION, game.phase());
		assertEquals("p4", game.pendingKnightId());
		assertEquals(T0.plusSeconds(20), game.deadline());

		game.chooseKnightTarget("p4", "p2");

		assertEquals(Phase.DEBATE, game.phase(), "Tras el golpe vuelve al dia");
		assertEquals(0, game.livesOf("p2"));
		assertEquals("p4", game.lastKnightStrike().knightId());
		assertEquals("p2", game.lastKnightStrike().targetId());
	}

	@Test
	void elGolpeNoSeMezclaConElParteDeLaNoche() {
		Game game = caballeroMuertoPorDosGolpes();
		game.advance(T0); // KNIGHT_REACTION

		assertEquals(1, game.dawnReport().events().size());
		assertEquals("p4", game.dawnReport().events().get(0).playerId());

		game.chooseKnightTarget("p4", "p2");

		assertEquals(1, game.dawnReport().events().size(), "El golpe se anuncia aparte, en el dia");
	}

	@Test
	void siElCaballeroNoEligeEnSuVentanaNoHayGolpe() {
		Game game = caballeroMuertoPorDosGolpes();
		game.advance(T0); // KNIGHT_REACTION

		game.advance(T0.plusSeconds(20));

		assertEquals(Phase.DEBATE, game.phase());
		assertNull(game.lastKnightStrike());
		assertNull(game.pendingKnightId());
	}

	@Test
	void soloElCaballeroPuedeGolpear() {
		Game game = caballeroMuertoPorDosGolpes();
		game.advance(T0); // KNIGHT_REACTION

		assertEquals(GameRuleException.NOT_YOUR_TURN,
				assertThrows(GameRuleException.class, () -> game.chooseKnightTarget("p1", "p2")).code());
	}

	@Test
	void elGolpePuedeQuitarLaUltimaVidaYDarLaVictoria() {
		Game game = caballeroMuertoDeNoche(0);
		game.advance(T0); // KNIGHT_REACTION

		game.chooseKnightTarget("p4", "p1");

		assertFalse(game.isAlive("p1"));
		assertEquals(Outcome.VILLAGE, game.outcome());
		assertEquals(Phase.FINISHED, game.phase());
	}

	@Test
	void elCaballeroLinchadoTambienGolpeaAntesDeLaNoche() {
		Game game = enVotacion(0);
		game.submitVote("p1", "p4");
		game.submitVote("p2", "p4");
		game.submitVote("p3", "p4");
		game.submitVote("p5", "p4");
		game.submitVote("p4", "p1");

		assertFalse(game.isAlive("p4"));
		assertEquals(Role.KNIGHT, game.lastLynch().revealedRole());
		assertEquals(Phase.KNIGHT_REACTION, game.phase());

		game.chooseKnightTarget("p4", "p2");

		assertFalse(game.isAlive("p2"));
		assertEquals(Phase.NIGHT, game.phase(), "El golpe de dia da paso a la noche");
		assertFalse(game.finished());
	}

	private static Game caballeroMuertoPorDosGolpes() {
		Game game = juego(1, true);
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (WOLVES)
		game.submitNightAction("p1", new WolfVote("p4"));
		game.advance(T0); // WOLVES -> WITCH
		game.submitNightAction("p6", new WitchAction(WitchPotion.POISON, "p4"));
		game.advance(T0); // WITCH -> DAWN
		return game;
	}

	private static Game caballeroMuertoDeNoche(int lives) {
		Game game = juego(lives, false);
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (WOLVES)
		game.submitNightAction("p1", new WolfVote("p4"));
		game.advance(T0); // NIGHT -> DAWN
		return game;
	}

	private static Game enVotacion(int lives) {
		Game game = juego(lives, false);
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (WOLVES)
		game.advance(T0); // NIGHT -> DAWN (sin votos)
		game.advance(T0); // DAWN -> DEBATE
		game.advance(T0); // DEBATE -> VOTING
		return game;
	}

	private static Game juego(int lives, boolean conBruja) {
		if (conBruja) {
			GameSettings settings = new GameSettings(1, List.of("knight", "witch"), lives, false, 240, 20, 45, true);
			Map<String, Role> roles = Map.of(
					"p1", Role.WOLF,
					"p2", Role.VILLAGER,
					"p3", Role.VILLAGER,
					"p4", Role.KNIGHT,
					"p5", Role.VILLAGER,
					"p6", Role.WITCH);
			return Game.startWithRoles("ABC234", settings, participants(6), roles, CLOCK, new Random(7));
		}

		GameSettings settings = new GameSettings(1, List.of("knight"), lives, false, 240, 20, 45, true);
		Map<String, Role> roles = Map.of(
				"p1", Role.WOLF,
				"p2", Role.VILLAGER,
				"p3", Role.VILLAGER,
				"p4", Role.KNIGHT,
				"p5", Role.VILLAGER);
		return Game.startWithRoles("ABC234", settings, participants(5), roles, CLOCK, new Random(7));
	}

	private static List<GameParticipant> participants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
