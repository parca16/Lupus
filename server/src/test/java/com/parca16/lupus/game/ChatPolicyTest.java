package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Visibilidad de los canales de chat por fase y estado (#63). */
class ChatPolicyTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void deDiaTodosLosVivosHablanEnPublico() {
		Game game = game();
		hasta(game, Phase.DEBATE);
		enamorar(game, "p3", "p4");

		assertTrue(ChatPolicy.canPost(game, "p3", ChatChannel.PUBLIC));
		assertTrue(ChatPolicy.canPost(game, "p1", ChatChannel.PUBLIC));
		assertFalse(ChatPolicy.canPost(game, "p3", ChatChannel.WOLVES));
		assertEquals(List.of(ChatChannel.PUBLIC, ChatChannel.LOVERS),
				ChatPolicy.available(game, "p3"));
	}

	@Test
	void deNocheSoloHablanLosLobos() {
		Game game = game();
		hasta(game, Phase.NIGHT);

		assertTrue(ChatPolicy.canPost(game, "p1", ChatChannel.WOLVES));
		assertFalse(ChatPolicy.canPost(game, "p3", ChatChannel.WOLVES));
		assertFalse(ChatPolicy.canPost(game, "p3", ChatChannel.PUBLIC));
		assertFalse(ChatPolicy.canPost(game, "p3", ChatChannel.LOVERS), "Los enamorados son p3 y p4");
	}

	@Test
	void losEnamoradosTienenCanalPrivadoSiempreQueSiganVivos() {
		Game game = game();
		hasta(game, Phase.NIGHT);
		enamorar(game, "p3", "p4");

		assertTrue(ChatPolicy.canPost(game, "p3", ChatChannel.LOVERS));
		assertTrue(ChatPolicy.canPost(game, "p4", ChatChannel.LOVERS));
		assertFalse(ChatPolicy.canPost(game, "p5", ChatChannel.LOVERS));
		assertEquals(List.of("p3", "p4"),
				ChatPolicy.recipients(game, ChatChannel.LOVERS).stream().map(GamePlayer::id).toList());
	}

	@Test
	void losMuertosNoEscribenEnNingunCanal() {
		Game game = game();
		hasta(game, Phase.DEBATE);
		enamorar(game, "p3", "p4");
		game.kill("p5");

		assertFalse(ChatPolicy.canPost(game, "p5", ChatChannel.PUBLIC));
		assertFalse(ChatPolicy.canPost(game, "p5", ChatChannel.WOLVES));
		assertFalse(ChatPolicy.canPost(game, "p5", ChatChannel.LOVERS));
	}

	@Test
	void losDestinatariosDependenDelCanal() {
		Game game = game();
		hasta(game, Phase.NIGHT);

		assertEquals(6, ChatPolicy.recipients(game, ChatChannel.PUBLIC).size());
		assertEquals(List.of("p1", "p2"),
				ChatPolicy.recipients(game, ChatChannel.WOLVES).stream().map(GamePlayer::id).toList());
	}

	private static Game game() {
		GameSettings settings = new GameSettings(2, List.of("seer", "witch"), 1, false, 240, 20, 45, true);
		Map<String, Role> roles = Map.of(
				"p1", Role.WOLF,
				"p2", Role.WOLF,
				"p3", Role.VILLAGER,
				"p4", Role.SEER,
				"p5", Role.WITCH,
				"p6", Role.VILLAGER);
		return Game.startWithRoles("ABC234", settings, participants(), roles, CLOCK, new Random(7));
	}

	private static void enamorar(Game game, String primero, String segundo) {
		for (GamePlayer player : game.players()) {
			if (player.id().equals(primero)) {
				player.loverId(segundo);
			}
			if (player.id().equals(segundo)) {
				player.loverId(primero);
			}
		}
	}

	private static void hasta(Game game, Phase phase) {
		for (int i = 0; i < 20 && game.phase() != phase; i++) {
			game.advance(T0);
		}
		assertEquals(phase, game.phase());
	}

	private static List<GameParticipant> participants() {
		return IntStream.rangeClosed(1, 6)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
