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
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class CupidTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void elCupidoEligeDosEnamoradosLaPrimeraNoche() {
		Game game = noche(1);

		game.submitNightAction("p3", new CupidAction("p2", "p4"));

		assertEquals(Optional.of("p4"), game.loverOf("p2"));
		assertEquals(Optional.of("p2"), game.loverOf("p4"));
		assertTrue(game.currentStepDone());
	}

	@Test
	void noPuedeElegirElMismoJugadorNiAMuertos() {
		Game game = noche(1);

		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p3", new CupidAction("p2", "p2"))).code());

		game.kill("p2");
		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p3", new CupidAction("p2", "p4"))).code());
	}

	@Test
	void siUnoMuereDeVerdadElOtroMuereDeAmor() {
		Game game = noche(0);
		game.submitNightAction("p3", new CupidAction("p2", "p4"));
		game.advance(T0); // CUPID -> WOLVES
		game.submitNightAction("p1", new WolfVote("p2"));
		game.advance(T0); // WOLVES -> DAWN

		assertFalse(game.isAlive("p2"));
		assertFalse(game.isAlive("p4"), "El otro enamorado muere al instante");
		assertEquals(2, game.dawnReport().events().stream()
				.filter(event -> event.type() == DawnEvent.Type.DIED)
				.count());
	}

	@Test
	void losEnamoradosNoPuedenVotarse() {
		Game game = noche(1);
		game.submitNightAction("p3", new CupidAction("p2", "p4"));
		game.advance(T0); // WOLVES (sin votos)
		game.advance(T0); // DAWN
		game.advance(T0); // DEBATE
		game.advance(T0); // VOTING

		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitVote("p2", "p4")).code());
	}

	@Test
	void losEnamoradosDeBandosDistintosGananSolos() {
		Game game = noche(0);
		game.submitNightAction("p3", new CupidAction("p1", "p2"));

		game.kill("p3");
		game.kill("p4");
		game.kill("p5");

		assertEquals(Outcome.LOVERS, game.checkVictory());
	}

	@Test
	void siSonDelMismoBandoNoCambianDeBando() {
		Game game = noche(0);
		game.submitNightAction("p3", new CupidAction("p2", "p4"));

		game.kill("p1");
		game.kill("p3");
		game.kill("p5");

		assertEquals(Outcome.VILLAGE, game.checkVictory());
	}

	private static Game noche(int lives) {
		GameSettings settings = new GameSettings(1, List.of("cupid"), lives, false, 240, 20, 45, true);
		Game game = Game.startWithRoles("ABC234", settings, participants(5), roles(), CLOCK, new Random(7));
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (CUPID)
		return game;
	}

	private static Map<String, Role> roles() {
		return Map.of(
				"p1", Role.WOLF,
				"p2", Role.VILLAGER,
				"p3", Role.CUPID,
				"p4", Role.VILLAGER,
				"p5", Role.VILLAGER);
	}

	private static List<GameParticipant> participants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
