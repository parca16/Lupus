package com.parca16.lupus.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.room.GameSettings;

class RoleDealerTest {

	private static final List<String> OCHO = List.of("a", "b", "c", "d", "e", "f", "g", "h");

	@Test
	void reparteUnaCartaPorJugadorConLaComposicionPedida() {
		GameSettings settings = new GameSettings(2, List.of("seer", "witch"), 1, false, 240, 20, 45, true);

		Map<String, Role> roles = RoleDealer.deal(settings, OCHO, new Random(7));

		assertEquals(8, roles.size());
		assertEquals(2, contar(roles, Role.WOLF));
		assertEquals(1, contar(roles, Role.SEER));
		assertEquals(1, contar(roles, Role.WITCH));
		assertEquals(4, contar(roles, Role.VILLAGER));
	}

	@Test
	void esDeterministaConLaMismaSemilla() {
		GameSettings settings = new GameSettings(2, List.of("seer", "witch"), 1, false, 240, 20, 45, true);

		Map<String, Role> primera = RoleDealer.deal(settings, OCHO, new Random(42));
		Map<String, Role> segunda = RoleDealer.deal(settings, OCHO, new Random(42));

		assertEquals(primera, segunda);
	}

	@Test
	void cadaJugadorRecibeExactamenteSuCarta() {
		GameSettings settings = new GameSettings(2, List.of("cupid"), 1, false, 240, 20, 45, true);

		Map<String, Role> roles = RoleDealer.deal(settings, OCHO, new Random(3));

		assertEquals(OCHO, List.copyOf(roles.keySet()));
		assertEquals(1, contar(roles, Role.CUPID));
	}

	private static long contar(Map<String, Role> roles, Role role) {
		return roles.values().stream().filter(actual -> actual == role).count();
	}
}
