package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class PresidentElectionTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void laMayoriaEligePresidenteAntesDelReparto() {
		Game game = eleccion();

		game.submitPresidentVote("p1", "p2");
		game.submitPresidentVote("p2", "p1");
		game.submitPresidentVote("p3", "p2");
		game.submitPresidentVote("p4", "p2");
		game.submitPresidentVote("p5", "p2");

		assertEquals("p2", game.presidentId());
		assertEquals(Phase.DEALING, game.phase());
		assertTrue(game.viewFor("p2").president());
	}

	@Test
	void elVotoEsSecretoHastaElRecuento() {
		Game game = eleccion();

		game.submitPresidentVote("p1", "p2");

		assertEquals(Optional.of(new Vote("p1", "p2")), game.presidentVoteOf("p1"));
		assertEquals(Optional.empty(), game.presidentVoteOf("p3"));
		assertEquals(null, game.presidentId());
	}

	@Test
	void noSePuedeVotarAUnoMismoNiAUnMuerto() {
		Game game = eleccion();

		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitPresidentVote("p1", "p1")).code());

		game.kill("p2");
		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitPresidentVote("p1", "p2")).code());
	}

	@Test
	void elEmpateLlevaASegundaVuelta() {
		Game game = eleccion();

		game.submitPresidentVote("p1", "p2");
		game.submitPresidentVote("p2", "p1");
		game.submitPresidentVote("p3", "p1");
		game.submitPresidentVote("p4", "p2");
		game.submitPresidentVote("p5", null);

		assertEquals(Phase.PRESIDENT_ELECTION, game.phase());
		assertTrue(game.presidentRunoffCandidates().containsAll(List.of("p1", "p2")));
		assertEquals(null, game.presidentId());
	}

	@Test
	void laSegundaVueltaSoloAdmiteEmpatados() {
		Game game = eleccion();
		empatar(game);

		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitPresidentVote("p5", "p3")).code());
	}

	@Test
	void siElEmpatePersisteSeSorteaEntreLosEmpatados() {
		Game game = eleccion();
		empatar(game);

		game.submitPresidentVote("p1", "p2");
		game.submitPresidentVote("p2", "p1");
		game.submitPresidentVote("p3", "p1");
		game.submitPresidentVote("p4", "p2");
		game.submitPresidentVote("p5", null);

		assertTrue(Set.of("p1", "p2").contains(game.presidentId()));
		assertEquals(Phase.DEALING, game.phase());
	}

	@Test
	void sinVotosSeSorteaEntreLosVivos() {
		Game game = eleccion();

		game.advance(T0.plusSeconds(45));

		assertTrue(Set.of("p1", "p2", "p3", "p4", "p5").contains(game.presidentId()));
		assertEquals(Phase.DEALING, game.phase());
	}

	@Test
	void elTickCierraLaEleccionAlVencerElPlazo() {
		Game game = eleccion();

		game.tick(T0.plusSeconds(45));

		assertTrue(game.presidentId() != null);
		assertTrue(game.phase() != Phase.PRESIDENT_ELECTION);
	}

	private static void empatar(Game game) {
		game.submitPresidentVote("p1", "p2");
		game.submitPresidentVote("p2", "p1");
		game.submitPresidentVote("p3", "p1");
		game.submitPresidentVote("p4", "p2");
		game.submitPresidentVote("p5", null);
	}

	private static Game eleccion() {
		GameSettings settings = new GameSettings(1, List.of(), 1, true, 240, 20, 45, true);
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
