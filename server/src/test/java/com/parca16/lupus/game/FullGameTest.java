package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Partida completa de principio a fin sobre el dominio (issue #45). */
class FullGameTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void partidaCompletaHastaLaVictoriaDelPueblo() {
		Game game = partida(1, 0);

		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT
		game.submitNightAction("p1", new WolfVote("p2"));
		game.advance(T0); // NIGHT -> DAWN

		assertEquals(Phase.DAWN, game.phase());
		assertEquals(1, game.dawnReport().events().size());
		assertEquals(DawnEvent.Type.DIED, game.dawnReport().events().get(0).type());
		assertEquals("p2", game.dawnReport().events().get(0).playerId());

		game.advance(T0); // DAWN -> DEBATE
		game.advance(T0); // DEBATE -> VOTING

		game.submitVote("p1", "p3");
		game.submitVote("p3", "p1");
		game.submitVote("p4", "p1");
		game.submitVote("p5", "p1");

		assertEquals("p1", game.lastVoteResult().lynchedId());
		assertTrue(game.lastLynch().died());
		assertEquals(Role.WOLF, game.lastLynch().revealedRole());
		assertEquals(Outcome.VILLAGE, game.outcome());
		assertEquals(Phase.FINISHED, game.phase());
		assertNull(game.deadline());
	}

	@Test
	void laParidadTrasLaNocheTerminaLaPartida() {
		Game game = partida(2, 0);

		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT
		game.submitNightAction("p1", new WolfVote("p2"));
		game.submitNightAction("p3", new WolfVote("p2"));
		game.advance(T0); // resuelve y comprueba victoria

		assertFalse(game.isAlive("p2"));
		assertEquals(Outcome.WOLVES, game.outcome());
		assertEquals(Phase.FINISHED, game.phase());
		assertNull(game.dawnReport(), "La partida termina antes de empezar el dia");
	}

	@Test
	void elParteDelAmanecerNoSeArrastraYLasFasesSiguenSuOrden() {
		Game game = partida(1, 1);

		game.advance(T0); // REVEAL
		game.advance(T0); // NIGHT
		game.advance(T0); // DAWN (sin votos)
		assertTrue(game.dawnReport().quiet());

		game.advance(T0); // DEBATE
		assertEquals(Phase.DEBATE, game.phase());

		game.markReadyToVote("p1");
		game.markReadyToVote("p2");
		game.markReadyToVote("p3");
		game.markReadyToVote("p4");
		game.markReadyToVote("p5");

		assertEquals(Phase.VOTING, game.phase());
	}

	private static Game partida(int lobos, int vidas) {
		GameSettings settings = new GameSettings(lobos, List.of(), vidas, false, 240, 20, 45, true);

		Map<String, Role> roles = lobos == 1
				? Map.of(
						"p1", Role.WOLF,
						"p2", Role.VILLAGER,
						"p3", Role.VILLAGER,
						"p4", Role.VILLAGER,
						"p5", Role.VILLAGER)
				: Map.of(
						"p1", Role.WOLF,
						"p2", Role.VILLAGER,
						"p3", Role.WOLF,
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
