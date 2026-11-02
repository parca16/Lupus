package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class DawnReportTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void unaNocheSinIncidentesSeAnunciaComoTranquila() {
		Game game = amanecerTranquilo(1);

		assertTrue(game.dawnReport().quiet());
		assertEquals(0, game.dawnReport().events().size());
	}

	@Test
	void unaMuerteNocturnaSeAnunciaSinRevelarElRol() {
		Game game = amanecerConAtaque(0, "p6");

		DawnEvent evento = game.dawnReport().events().get(0);

		assertEquals(DawnEvent.Type.DIED, evento.type());
		assertEquals("p6", evento.playerId());
		assertEquals(0, evento.livesLeft());

		Set<String> campos = Arrays.stream(DawnEvent.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(Collectors.toSet());
		assertFalse(campos.contains("role"), "El parte no puede revelar roles (D7)");
	}

	@Test
	void unaVidaPerdidaSeAnunciaConLasRestantes() {
		Game game = amanecerConAtaque(1, "p6");

		DawnEvent evento = game.dawnReport().events().get(0);

		assertEquals(DawnEvent.Type.LOST_LIFE, evento.type());
		assertEquals("p6", evento.playerId());
		assertEquals(0, evento.livesLeft());
		assertTrue(game.isAlive("p6"));
	}

	@Test
	void dosGolpesEnLaMismaNocheSeResumenEnUnaMuerte() {
		Game game = noche(1);
		game.advance(T0); // GUARDIAN
		game.advance(T0); // WOLVES
		game.submitNightAction("p1", new WolfVote("p6"));
		game.submitNightAction("p2", new WolfVote("p6"));
		game.advance(T0); // WITCH
		game.submitNightAction("p5", new WitchAction(WitchPotion.POISON, "p6"));
		game.advance(T0); // DAWN

		assertEquals(1, game.dawnReport().events().size());
		assertEquals(DawnEvent.Type.DIED, game.dawnReport().events().get(0).type());
	}

	@Test
	void protegerALaVictimaDejaLaNocheTranquila() {
		Game game = noche(1);
		game.advance(T0); // GUARDIAN
		game.submitNightAction("p4", new GuardianAction("p6"));
		game.advance(T0); // WOLVES
		game.submitNightAction("p1", new WolfVote("p6"));
		game.submitNightAction("p2", new WolfVote("p6"));
		game.advance(T0); // WITCH
		game.advance(T0); // DAWN

		assertTrue(game.dawnReport().quiet());
	}

	@Test
	void curarUnaVidaPerdidaSeAnunciaComoRecuperada() {
		Game game = amanecerConAtaque(1, "p6");
		assertEquals(DawnEvent.Type.LOST_LIFE, game.dawnReport().events().get(0).type());

		game.advance(T0); // DEBATE
		game.advance(T0); // VOTING
		game.advance(T0); // NIGHT 2 (SEER)
		game.advance(T0); // GUARDIAN
		game.advance(T0); // WOLVES (sin votos)
		game.advance(T0); // WITCH
		game.submitNightAction("p5", new WitchAction(WitchPotion.HEAL, "p6"));
		game.advance(T0); // DAWN

		DawnEvent evento = game.dawnReport().events().get(0);
		assertEquals(DawnEvent.Type.HEALED, evento.type());
		assertEquals(1, evento.livesLeft());
	}

	@Test
	void elParteNoSeArrastraEntreNoches() {
		Game game = amanecerConAtaque(0, "p6");
		assertEquals(1, game.dawnReport().events().size());

		game.advance(T0); // DEBATE
		game.advance(T0); // VOTING
		game.advance(T0); // NIGHT 2

		assertNull(game.dawnReport());
	}

	private static Game amanecerTranquilo(int lives) {
		Game game = noche(lives);
		game.advance(T0); // GUARDIAN
		game.advance(T0); // WOLVES (sin votos)
		game.advance(T0); // WITCH
		game.advance(T0); // DAWN
		return game;
	}

	private static Game amanecerConAtaque(int lives, String objetivo) {
		Game game = noche(lives);
		game.advance(T0); // GUARDIAN
		game.advance(T0); // WOLVES
		game.submitNightAction("p1", new WolfVote(objetivo));
		game.submitNightAction("p2", new WolfVote(objetivo));
		game.advance(T0); // WITCH
		game.advance(T0); // DAWN
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
