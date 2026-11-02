package com.parca16.lupus.room;

import java.util.List;
import java.util.Set;

/**
 * Configuracion de la partida que decide el host.
 *
 * <p>El numero de cartas siempre coincide con el de jugadores: los lobos y los roles especiales
 * eligen cartas y el resto se rellena con aldeanos (issue #27).
 */
public record GameSettings(int wolves, List<String> roles, int lives, boolean president,
		int debateSeconds, int roleSeconds, int voteSeconds, boolean showVoteDetail) {

	public static final Set<String> KNOWN_ROLES = Set.of("seer", "witch", "knight", "guardian", "cupid");
	public static final int MIN_PLAYERS = 5;
	public static final int MAX_WOLVES = 8;

	public static final GameSettings DEFAULT = new GameSettings(1, List.of(), 1, false, 240, 20, 45, true);

	public GameSettings {
		roles = List.copyOf(roles);
	}

	/** Valida la configuracion en si misma y devuelve esta instancia. */
	public GameSettings validated() {
		if (wolves < 1 || wolves > MAX_WOLVES) {
			throw new RoomException(RoomException.INVALID_SETTINGS,
					"El numero de lobos debe estar entre 1 y " + MAX_WOLVES + ".");
		}
		if (roles.size() != roles.stream().distinct().count()) {
			throw new RoomException(RoomException.INVALID_SETTINGS, "No se puede repetir un rol especial.");
		}
		if (!KNOWN_ROLES.containsAll(roles)) {
			throw new RoomException(RoomException.INVALID_SETTINGS, "Hay un rol especial desconocido.");
		}
		if (lives < 0 || lives > 2) {
			throw new RoomException(RoomException.INVALID_SETTINGS, "Las vidas deben ser 0, 1 o 2.");
		}
		if (debateSeconds < 30 || debateSeconds > 600) {
			throw new RoomException(RoomException.INVALID_SETTINGS,
					"El tiempo de debate debe estar entre 30 y 600 segundos.");
		}
		if (roleSeconds < 10 || roleSeconds > 120) {
			throw new RoomException(RoomException.INVALID_SETTINGS,
					"El tiempo de accion de rol debe estar entre 10 y 120 segundos.");
		}
		if (voteSeconds < 15 || voteSeconds > 180) {
			throw new RoomException(RoomException.INVALID_SETTINGS,
					"El tiempo de votacion debe estar entre 15 y 180 segundos.");
		}
		return this;
	}

	/** Valida que la configuracion se pueda repartir con ese numero de jugadores. */
	public GameSettings validatedForStart(int players) {
		validated();

		if (players < MIN_PLAYERS) {
			throw new RoomException(RoomException.NOT_ENOUGH_PLAYERS,
					"Hacen falta al menos " + MIN_PLAYERS + " jugadores para empezar (hay " + players + ").");
		}

		int special = wolves + roles.size();
		if (special >= players) {
			throw new RoomException(RoomException.INVALID_SETTINGS_FOR_PLAYERS,
					"Con " + players + " jugadores no caben " + special
							+ " cartas especiales: debe quedar al menos 1 aldeano.");
		}

		return this;
	}

	/** Cuantos aldeanos rellenan el reparto para que haya una carta por jugador. */
	public int villagers(int players) {
		return players - wolves - roles.size();
	}
}
