package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;
import com.parca16.lupus.room.RoomException;

class GameTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void sinPresidenteEmpiezaRepartiendoUnaCartaPorJugador() {
		Game game = Game.start("ABC234", settings(false, List.of("seer")), participants(5), CLOCK, new Random(1));

		assertEquals(Phase.DEALING, game.phase());
		assertEquals(T0, game.deadline());
		assertEquals(5, game.viewFor("p1").players().size());
		assertEquals(2, contar(game, Role.WOLF, 5));
		assertEquals(1, contar(game, Role.SEER, 5));
		assertEquals(2, contar(game, Role.VILLAGER, 5));

		for (int i = 1; i <= 5; i++) {
			assertNotNull(game.viewFor("p" + i).role(), "Todos deben tener carta tras el reparto");
			assertEquals(1, game.viewFor("p" + i).lives());
			assertTrue(game.viewFor("p" + i).alive());
		}
	}

	@Test
	void conPresidentePrimeroSeEligeYDespuesSeReparte() {
		Game game = Game.start("ABC234", settings(true, List.of("seer")), participants(5), CLOCK, new Random(1));

		assertEquals(Phase.PRESIDENT_ELECTION, game.phase());
		assertEquals(T0.plus(Game.PRESIDENT_ELECTION_TIME), game.deadline());
		assertNull(game.viewFor("p1").role(), "Los roles no se reparten antes de elegir presidente");
		assertThrows(IllegalStateException.class, () -> game.advance(T0));

		game.electPresident("p2");

		assertEquals(Phase.DEALING, game.phase());
		assertTrue(game.viewFor("p2").president());
		assertEquals("p2", game.presidentId());
		assertNotNull(game.viewFor("p1").role());
	}

	@Test
	void noSePuedeElegirPresidenteFueraDeLaFaseOCuandoNoExiste() {
		Game sinPresidente = Game.start("ABC234", settings(false, List.of()),
				participants(5), CLOCK, new Random(1));
		assertThrows(IllegalStateException.class, () -> sinPresidente.electPresident("p1"));

		Game conPresidente = Game.start("ABC234", settings(true, List.of()),
				participants(5), CLOCK, new Random(1));
		assertThrows(IllegalArgumentException.class, () -> conPresidente.electPresident("no-existe"));
	}

	@Test
	void laVistaDeUnJugadorNoExponeLosRolesDeLosDemas() {
		Game game = Game.start("ABC234", settings(false, List.of("seer")), participants(5), CLOCK, new Random(1));

		GameView vista = game.viewFor("p1");

		assertNotNull(vista.role());
		Set<String> camposPublicos = Arrays.stream(GameView.PublicPlayer.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(Collectors.toSet());
		assertEquals(Set.of("id", "nickname", "lives", "alive", "president"), camposPublicos,
				"La vista publica no debe incluir el rol");

		Set<String> camposVista = Arrays.stream(GameView.class.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(Collectors.toSet());
		assertFalse(camposVista.contains("roles"), "La vista solo puede traer el rol propio");
	}

	@Test
	void laRevelacionDuraDiezSegundosYAvanzaConElPlazo() {
		Game game = Game.start("ABC234", settings(false, List.of()), participants(5), CLOCK, new Random(1));

		game.tick(T0);

		assertEquals(Phase.REVEAL, game.phase());
		assertEquals(T0.plus(Game.REVEAL_TIME), game.deadline());

		game.tick(T0.plusSeconds(9));
		assertEquals(Phase.REVEAL, game.phase());

		game.tick(T0.plus(Game.REVEAL_TIME));
		assertEquals(Phase.NIGHT, game.phase());
	}

	@Test
	void laNocheRecorreSoloLosPasosDeLosRolesEnJuego() {
		Game game = Game.start("ABC234", settings(false, List.of("seer", "witch")), participants(5), CLOCK,
				new Random(1));

		game.tick(T0);
		game.advance(T0);

		assertEquals(NightStep.SEER, game.nightStep());
		game.advance(T0);
		assertEquals(NightStep.WOLVES, game.nightStep(), "Sin Guardian, tras el Vidente van los lobos");
		game.advance(T0);
		assertEquals(NightStep.WITCH, game.nightStep());
		game.advance(T0);
		assertEquals(Phase.DAWN, game.phase());
		assertNull(game.nightStep());
	}

	@Test
	void cupidoSoloActuaLaPrimeraNoche() {
		Game game = Game.start("ABC234", settings(false, List.of("cupid")), participants(5), CLOCK, new Random(1));

		game.tick(T0);
		game.advance(T0);

		assertEquals(NightStep.CUPID, game.nightStep());
		game.advance(T0);
		assertEquals(NightStep.WOLVES, game.nightStep());
		game.advance(T0);
		assertEquals(Phase.DAWN, game.phase());

		game.advance(T0);
		assertEquals(Phase.DEBATE, game.phase());
		game.advance(T0);
		assertEquals(Phase.VOTING, game.phase());
		game.advance(T0);

		assertEquals(Phase.NIGHT, game.phase());
		assertEquals(2, game.round());
		assertEquals(NightStep.WOLVES, game.nightStep(), "Cupido no vuelve a actuar");
	}

	@Test
	void losTiemposSalenDeLaConfiguracionYDelReglamento() {
		Game game = Game.start("ABC234", settings(false, List.of("seer")), participants(5), CLOCK, new Random(1));

		game.tick(T0);
		assertEquals(T0.plusSeconds(10), game.deadline(), "Revelacion: 10 s");

		game.advance(T0);
		assertEquals(NightStep.SEER, game.nightStep());
		assertEquals(T0.plusSeconds(20), game.deadline(), "Accion de rol: 20 s");

		game.advance(T0);
		assertEquals(NightStep.WOLVES, game.nightStep());
		assertEquals(T0.plusSeconds(40), game.deadline(), "Lobos: minimo 40 s");

		game.advance(T0);
		assertEquals(T0.plusSeconds(10), game.deadline(), "Amanecer: 10 s");

		game.advance(T0);
		assertEquals(T0.plusSeconds(240), game.deadline(), "Debate: 240 s");

		game.advance(T0);
		assertEquals(T0.plusSeconds(45), game.deadline(), "Votacion: 45 s");

		game.startRunoff();
		assertEquals(T0.plusSeconds(30), game.deadline(), "Segunda vuelta: 30 s");
	}

	@Test
	void finishCierraLaPartida() {
		Game game = Game.start("ABC234", settings(false, List.of()), participants(5), CLOCK, new Random(1));

		game.finish(Outcome.WOLVES);

		assertEquals(Phase.FINISHED, game.phase());
		assertTrue(game.finished());
		assertEquals(Outcome.WOLVES, game.outcome());
		assertNull(game.deadline());
		assertNull(game.nightStep());
		assertThrows(IllegalStateException.class, () -> game.advance(T0));
	}

	@Test
	void noSePuedeEmpezarConMenosDeCincoJugadores() {
		RoomException exception = assertThrows(RoomException.class,
				() -> Game.start("ABC234", settings(false, List.of()), participants(4), CLOCK, new Random(1)));

		assertEquals(RoomException.NOT_ENOUGH_PLAYERS, exception.code());
	}

	private static GameSettings settings(boolean president, List<String> roles) {
		return new GameSettings(2, roles, 1, president, 240, 20, 45, true);
	}

	private static List<GameParticipant> participants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}

	private static long contar(Game game, Role role, int players) {
		long total = 0;
		for (int i = 1; i <= players; i++) {
			if (game.viewFor("p" + i).role() == role) {
				total++;
			}
		}
		return total;
	}
}
