package com.parca16.lupus.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import com.parca16.lupus.room.GameSettings;

/** Reparto aleatorio de una carta por jugador (issue #35). */
public final class RoleDealer {

	private RoleDealer() {
	}

	/**
	 * Reparte lobos, roles especiales y aldeanos hasta completar una carta por jugador.
	 *
	 * <p>El reparto es aleatorio y determinista para una misma semilla, lo que permite testearlo.
	 */
	public static Map<String, Role> deal(GameSettings settings, List<String> playerIds, RandomGenerator random) {
		settings.validatedForStart(playerIds.size());

		List<Role> deck = new ArrayList<>();
		for (int i = 0; i < settings.wolves(); i++) {
			deck.add(Role.WOLF);
		}
		for (String special : settings.roles()) {
			deck.add(Role.fromSettingsId(special));
		}
		while (deck.size() < playerIds.size()) {
			deck.add(Role.VILLAGER);
		}

		for (int i = deck.size() - 1; i > 0; i--) {
			Collections.swap(deck, i, random.nextInt(i + 1));
		}

		Map<String, Role> assigned = new LinkedHashMap<>();
		for (int i = 0; i < playerIds.size(); i++) {
			assigned.put(playerIds.get(i), deck.get(i));
		}
		return assigned;
	}
}
