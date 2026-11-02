package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class VotingTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void elVotoEsSecretoHastaElRecuento() {
		Game game = enVotacion(1, false);

		game.submitVote("p1", "p6");

		assertEquals(Optional.of(new Vote("p1", "p6")), game.voteOf("p1"));
		assertEquals(Optional.empty(), game.voteOf("p2"), "Cada jugador solo ve su voto");
		assertNull(game.lastVoteResult(), "No hay recuento hasta cerrar");
		assertNull(game.lastLynch());
	}

	@Test
	void alVotarTodosSeCuentaYSeAplicanLasVidas() {
		Game game = enVotacion(1, false);

		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p1");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");
		game.submitVote("p6", "p2");

		assertEquals("p1", game.lastVoteResult().lynchedId());
		assertEquals(Phase.NIGHT, game.phase());
		assertTrue(game.lastLynch().survived());
		assertFalse(game.lastLynch().died());
		assertNull(game.lastLynch().revealedRole(), "Si sobrevive no se revela el rol");
		assertEquals(0, game.livesOf("p1"));
	}

	@Test
	void laAbstencionNoSumaANadie() {
		Game game = enVotacion(1, false);

		game.submitVote("p1", "p6");
		game.submitVote("p2", null);
		game.submitVote("p3", "p6");
		game.submitVote("p4", null);
		game.submitVote("p5", "p6");
		game.submitVote("p6", null);

		assertEquals(3, game.lastVoteResult().abstentions());
		assertEquals(3, game.lastVoteResult().counts().get("p6"));
		assertEquals("p6", game.lastVoteResult().lynchedId());
	}

	@Test
	void elEmpateLlevaASegundaVueltaEntreLosEmpatados() {
		Game game = enVotacion(1, false);

		votarEmpate(game);

		assertEquals(Phase.RUNOFF, game.phase());
		assertEquals(2, game.runoffCandidates().size());
		assertTrue(game.runoffCandidates().containsAll(List.of("p1", "p6")));
	}

	@Test
	void enLaSegundaVueltaSoloSeVotaALosEmpatados() {
		Game game = enVotacion(1, false);
		votarEmpate(game);

		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitVote("p5", "p3")).code());
	}

	@Test
	void siElEmpatePersisteNadieMuere() {
		Game game = enVotacion(1, false);
		votarEmpate(game);

		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p6");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");
		game.submitVote("p6", "p1");

		assertNull(game.lastVoteResult().lynchedId());
		assertNull(game.lastLynch().targetId());
		assertEquals(Phase.NIGHT, game.phase());
	}

	@Test
	void elVotoDelPresidenteValeDoble() {
		Game game = enVotacionConPresidente(1);

		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p1");
		game.submitVote("p4", "p1");
		game.submitVote("p5", null);
		game.submitVote("p6", null);

		assertEquals("p1", game.lastVoteResult().lynchedId(),
				"Con el voto doble del Presidente, p1 gana 3 a 2");
		assertFalse(game.lastVoteResult().runoffNeeded());
		assertEquals(3, game.lastVoteResult().counts().get("p1"));
	}

	@Test
	void elDetalleDeVotosSoloSePublicaSiEstaActivado() {
		Game conDetalle = enVotacion(1, true);
		Game sinDetalle = enVotacion(1, false);

		votarTodos(conDetalle);
		votarTodos(sinDetalle);

		assertEquals(6, conDetalle.lastVoteResult().details().size());
		assertTrue(sinDetalle.lastVoteResult().details().isEmpty());
	}

	@Test
	void noSePuedeVotarAUnoMismoNiAUnMuerto() {
		Game game = enVotacion(1, false);

		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitVote("p1", "p1")).code());

		game.kill("p6");
		assertEquals(GameRuleException.INVALID_TARGET,
				assertThrows(GameRuleException.class, () -> game.submitVote("p1", "p6")).code());
	}

	@Test
	void elTickCierraLaVotacionAlVencerElPlazo() {
		Game game = enVotacion(1, false);
		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");

		game.tick(T0.plusSeconds(45));

		assertEquals("p6", game.lastVoteResult().lynchedId());
		assertEquals(Phase.NIGHT, game.phase());
	}

	private static void votarEmpate(Game game) {
		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p6");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");
		game.submitVote("p6", "p1");
	}

	private static void votarTodos(Game game) {
		game.submitVote("p1", "p6");
		game.submitVote("p2", "p6");
		game.submitVote("p3", "p1");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");
		game.submitVote("p6", "p2");
	}

	private static Game enVotacion(int lives, boolean showDetail) {
		Game game = game(lives, showDetail, false);
		hastaVotacion(game);
		return game;
	}

	private static Game enVotacionConPresidente(int lives) {
		Game game = game(lives, false, true);
		game.electPresident("p3");
		hastaVotacion(game);
		return game;
	}

	private static void hastaVotacion(Game game) {
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT
		game.advance(T0); // SEER -> GUARDIAN (o WOLVES si no hay especiales)
		game.advance(T0); // GUARDIAN -> WOLVES (o DAWN si no hay)
		game.advance(T0); // WOLVES -> WITCH (o DAWN)
		game.advance(T0); // WITCH -> DAWN (o DAWN ya)
		game.advance(T0); // DAWN -> DEBATE
		game.advance(T0); // DEBATE -> VOTING
	}

	private static Game game(int lives, boolean showDetail, boolean president) {
		GameSettings settings = new GameSettings(2, List.of("seer", "guardian", "witch"), lives, president, 240, 20, 45,
				showDetail);
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
