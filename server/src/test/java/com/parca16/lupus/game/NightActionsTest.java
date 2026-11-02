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
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class NightActionsTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void elVidenteVeElRolYSoloActuaEnSuPaso() {
		Game game = noche(1);

		game.submitNightAction("p3", new SeerAction("p1"));

		assertEquals(Optional.of(Role.WOLF), game.seerVision("p3"));
		assertEquals(Optional.empty(), game.seerVision("p4"), "Solo el Vidente ve roles");

		assertEquals(GameRuleException.ALREADY_ACTED,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p3", new SeerAction("p2"))).code());

		assertEquals(GameRuleException.NOT_YOUR_TURN,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p4", new GuardianAction("p6"))).code());
	}

	@Test
	void losLobosDecidenPorMayoriaYAplicanElAtaque() {
		Game game = enPasoLobos(noche(1));

		game.submitNightAction("p1", new WolfVote("p6"));
		game.submitNightAction("p2", new WolfVote("p6"));

		assertTrue(game.currentStepDone());
		game.advance(T0);

		assertEquals(Optional.of("p6"), game.wolfTargetFor("p5"));
		assertEquals(Optional.empty(), game.wolfTargetFor("p6"), "Solo la Bruja ve la victima");

		game.advance(T0);

		assertEquals(0, game.livesOf("p6"));
		assertTrue(game.isAlive("p6"), "Con una vida pierde la vida pero sobrevive");
		assertEquals("p6", game.lastNight().wolfTargetId());
		assertFalse(game.lastNight().wolfBlocked());
	}

	@Test
	void elEmpateDeLobosSeResuelveAlAzar() {
		Game game = enPasoLobos(noche(1));

		game.submitNightAction("p1", new WolfVote("p6"));
		game.submitNightAction("p2", new WolfVote("p3"));
		game.advance(T0);

		String objetivo = game.wolfTargetFor("p5").orElseThrow();

		assertTrue(Set.of("p6", "p3").contains(objetivo), "Debe salir uno de los empatados: " + objetivo);
	}

	@Test
	void losLobosPuedenAtacarAOtroLoboOASiMismos() {
		Game game = enPasoLobos(noche(1));

		game.submitNightAction("p1", new WolfVote("p1"));
		game.submitNightAction("p2", new WolfVote("p1"));
		game.advance(T0);
		game.advance(T0);

		assertEquals(0, game.livesOf("p1"));
		assertTrue(game.isAlive("p1"));
	}

	@Test
	void sinVotosNoHayAtaque() {
		Game game = enPasoLobos(noche(1));

		game.advance(T0);
		game.advance(T0);

		assertEquals(Optional.empty(), game.wolfTargetFor("p5"));
		assertTrue(game.lastNight().events().isEmpty());
		assertEquals(1, game.livesOf("p6"));
	}

	@Test
	void elGuardianProtegeDelAtaqueDeLosLobos() {
		Game game = noche(1);
		game.advance(T0);
		game.submitNightAction("p4", new GuardianAction("p6"));

		game.advance(T0);
		game.submitNightAction("p1", new WolfVote("p6"));
		game.submitNightAction("p2", new WolfVote("p6"));
		game.advance(T0);
		game.advance(T0);

		assertEquals(1, game.livesOf("p6"));
		assertTrue(game.isAlive("p6"));
		assertTrue(game.lastNight().wolfBlocked());
		assertTrue(game.lastNight().events().stream()
				.anyMatch(event -> event.type() == NightEvent.Type.WOLF_ATTACK && event.blocked()));
	}

	@Test
	void elGuardianNoPuedeRepetirObjetivoEnNochesConsecutivas() {
		Game game = noche(1);
		game.advance(T0);
		game.submitNightAction("p4", new GuardianAction("p6"));
		terminarNocheDesdeGuardian(game);

		game.advance(T0);
		assertEquals(GameRuleException.REPEATED_TARGET,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p4", new GuardianAction("p6"))).code());
	}

	@Test
	void elGuardianTieneTresProteccionesPorPartida() {
		Game game = noche(1);
		game.advance(T0);
		game.submitNightAction("p4", new GuardianAction("p6"));
		terminarNocheDesdeGuardian(game);

		game.advance(T0);
		game.submitNightAction("p4", new GuardianAction("p1"));
		terminarNocheDesdeGuardian(game);

		game.advance(T0);
		game.submitNightAction("p4", new GuardianAction("p6"));
		terminarNocheDesdeGuardian(game);

		game.advance(T0);
		assertEquals(GameRuleException.GUARDIAN_LIMIT,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p4", new GuardianAction("p1"))).code());
	}

	@Test
	void laBrujaVeLaVictimaYLaCura() {
		Game game = enPasoLobos(noche(1));
		game.submitNightAction("p1", new WolfVote("p6"));
		game.submitNightAction("p2", new WolfVote("p6"));
		game.advance(T0);

		assertEquals(Optional.of("p6"), game.wolfTargetFor("p5"));
		game.submitNightAction("p5", new WitchAction(WitchPotion.HEAL, "p6"));
		game.advance(T0);

		assertEquals(1, game.livesOf("p6"));
		assertTrue(game.isAlive("p6"));
		assertTrue(game.lastNight().events().stream()
				.anyMatch(event -> event.type() == NightEvent.Type.HEAL && event.applied()));
	}

	@Test
	void elGuardianTambienBloqueaElVeneno() {
		Game game = noche(1);
		game.advance(T0);
		game.submitNightAction("p4", new GuardianAction("p6"));
		game.advance(T0);
		game.submitNightAction("p1", new WolfVote("p3"));
		game.submitNightAction("p2", new WolfVote("p3"));
		game.advance(T0);

		game.submitNightAction("p5", new WitchAction(WitchPotion.POISON, "p6"));
		game.advance(T0);

		assertEquals(1, game.livesOf("p6"));
		assertTrue(game.lastNight().events().stream()
				.anyMatch(event -> event.type() == NightEvent.Type.POISON && event.blocked()));
	}

	@Test
	void laBrujaNoActuaSiMuereEsaNoche() {
		Game game = enPasoLobos(noche(0));
		game.submitNightAction("p1", new WolfVote("p5"));
		game.submitNightAction("p2", new WolfVote("p5"));
		game.advance(T0);

		game.submitNightAction("p5", new WitchAction(WitchPotion.HEAL, "p6"));
		game.advance(T0);

		assertFalse(game.isAlive("p5"), "Con 0 vidas el ataque de los lobos la mata");
		assertTrue(game.lastNight().events().stream()
				.noneMatch(event -> event.type() == NightEvent.Type.HEAL),
				"Su pocion no debe aplicarse");
	}

	@Test
	void laBrujaSoloPuedeUsarUnaPocionPorNoche() {
		Game game = enPasoLobos(noche(1));
		game.advance(T0);

		game.submitNightAction("p5", new WitchAction(WitchPotion.HEAL, "p6"));

		assertEquals(GameRuleException.ALREADY_ACTED,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p5", new WitchAction(WitchPotion.POISON, "p6"))).code());
	}

	@Test
	void cadaPocionSeGastaUnaSolaVezPorPartida() {
		Game game = noche(1);
		game.advance(T0);
		game.advance(T0);
		game.advance(T0);
		game.submitNightAction("p5", new WitchAction(WitchPotion.HEAL, "p6"));
		terminarNocheDesdeBruja(game);

		game.advance(T0);
		game.advance(T0);
		game.advance(T0);

		assertEquals(GameRuleException.NO_POTIONS,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p5", new WitchAction(WitchPotion.HEAL, "p6"))).code());
	}

	@Test
	void lasAccionesSeReinicianCadaNoche() {
		Game game = noche(1);
		game.submitNightAction("p3", new SeerAction("p1"));
		assertEquals(Optional.of(Role.WOLF), game.seerVision("p3"));

		terminarNocheDesdeVidente(game);

		assertEquals(Optional.empty(), game.seerVision("p3"), "La vision de la noche anterior no se arrastra");
	}

	@Test
	void noSePuedeActuarFueraDeLaNoche() {
		Game game = game(1);
		game.advance(T0); // DEALING -> REVEAL

		assertEquals(GameRuleException.NOT_YOUR_TURN,
				assertThrows(GameRuleException.class,
						() -> game.submitNightAction("p3", new SeerAction("p1"))).code());
	}

	private static Game noche(int lives) {
		Game game = game(lives);
		game.advance(T0); // DEALING -> REVEAL
		game.advance(T0); // REVEAL -> NIGHT (SEER)
		return game;
	}

	private static Game enPasoLobos(Game game) {
		game.advance(T0); // SEER -> GUARDIAN
		game.advance(T0); // GUARDIAN -> WOLVES
		return game;
	}

	/** Desde el paso del Guardian (con su accion ya enviada) hasta la noche siguiente. */
	private static void terminarNocheDesdeGuardian(Game game) {
		game.advance(T0); // WOLVES
		game.advance(T0); // WITCH
		terminarNocheDesdeBruja(game);
	}

	private static void terminarNocheDesdeBruja(Game game) {
		game.advance(T0); // DAWN
		game.advance(T0); // DEBATE
		game.advance(T0); // VOTING
		game.advance(T0); // NIGHT (SEER)
	}

	private static void terminarNocheDesdeVidente(Game game) {
		game.advance(T0); // GUARDIAN
		game.advance(T0); // WOLVES
		game.advance(T0); // WITCH
		terminarNocheDesdeBruja(game);
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
