package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class GameLivesTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

	@Test
	void lasVidasInicialesSonLasDeLaConfiguracion() {
		assertEquals(0, game(0).livesOf("p1"));
		assertEquals(1, game(1).livesOf("p1"));
		assertEquals(2, game(2).livesOf("p1"));
	}

	@Test
	void conDosVidasSeSobreviveADosGolpesYElTerceroMata() {
		Game game = game(2);

		assertEquals(HitResult.LOST_LIFE, game.hit("p1"));
		assertEquals(1, game.livesOf("p1"));
		assertTrue(game.isAlive("p1"));

		assertEquals(HitResult.LOST_LIFE, game.hit("p1"));
		assertEquals(0, game.livesOf("p1"));
		assertTrue(game.isAlive("p1"), "Llegar a 0 vidas no es morir: el siguiente golpe mata");

		assertEquals(HitResult.DIED, game.hit("p1"));
		assertFalse(game.isAlive("p1"));
	}

	@Test
	void conUnaVidaElPrimerGolpeDejaCeroVidasYSobrevive() {
		Game game = game(1);

		assertEquals(HitResult.LOST_LIFE, game.hit("p1"));
		assertEquals(0, game.livesOf("p1"));
		assertTrue(game.isAlive("p1"), "Si tiene vidas restantes, sobrevive al golpe (#43)");

		assertEquals(HitResult.DIED, game.hit("p1"));
	}

	@Test
	void conCeroVidasCualquierGolpeEsMuerteDefinitiva() {
		Game game = game(0);

		assertEquals(HitResult.DIED, game.hit("p1"));
		assertFalse(game.isAlive("p1"));
	}

	@Test
	void unGolpeSobreUnMuertoNoCambiaNada() {
		Game game = game(0);
		game.hit("p1");

		assertEquals(HitResult.ALREADY_DEAD, game.hit("p1"));
		assertEquals(0, game.livesOf("p1"));
	}

	@Test
	void lasVidasRestantesSonPublicas() {
		Game game = game(2);
		game.hit("p1");

		GameView.PublicPlayer visto = game.viewFor("p2").players().stream()
				.filter(player -> player.id().equals("p1"))
				.findFirst()
				.orElseThrow();

		assertEquals(1, visto.lives());
		assertTrue(visto.alive());
	}

	@Test
	void curarRestauraUnaVidaSinSuperarElMaximo() {
		Game game = game(2);
		game.hit("p1");

		assertTrue(game.heal("p1"));
		assertEquals(2, game.livesOf("p1"));

		assertFalse(game.heal("p1"), "No puede superar el maximo configurado");
		assertEquals(2, game.livesOf("p1"));
	}

	@Test
	void curarNoHaceNadaConUnMuerto() {
		Game game = game(1);
		game.hit("p1");
		game.hit("p1");

		assertFalse(game.isAlive("p1"));
		assertFalse(game.heal("p1"));
	}

	@Test
	void killIgnoraLasVidasYDejaAlJugadorMuerto() {
		Game game = game(2);

		assertTrue(game.kill("p1"));
		assertFalse(game.isAlive("p1"));
		assertEquals(0, game.livesOf("p1"));
		assertFalse(game.kill("p1"), "Ya estaba muerto");
	}

	@Test
	void noSePuedeGolpearAUnJugadorQueNoExiste() {
		Game game = game(1);

		assertThrows(IllegalArgumentException.class, () -> game.hit("fantasma"));
		assertThrows(IllegalArgumentException.class, () -> game.heal("fantasma"));
		assertThrows(IllegalArgumentException.class, () -> game.kill("fantasma"));
	}

	private static Game game(int lives) {
		GameSettings settings = new GameSettings(1, List.of(), lives, false, 240, 20, 45, true);
		return Game.start("ABC234", settings, participants(5), CLOCK, new Random(1));
	}

	private static List<GameParticipant> participants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
