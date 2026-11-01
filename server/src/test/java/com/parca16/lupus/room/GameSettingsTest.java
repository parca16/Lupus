package com.parca16.lupus.room;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameSettingsTest {

	private static GameSettings conLobos(int wolves) {
		return new GameSettings(wolves, List.of(), 1, false, 120, 30, 45, true);
	}

	@Test
	void laConfiguracionPorDefectoEsValida() {
		assertDoesNotThrow(() -> GameSettings.DEFAULT.validated());
	}

	@Test
	void rechazaNumerosDeLobosImposibles() {
		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, () -> conLobos(0).validated()).code());
		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, () -> conLobos(9).validated()).code());
	}

	@Test
	void rechazaRolesRepetidos() {
		GameSettings settings = new GameSettings(1, List.of("seer", "seer"), 1, false, 120, 30, 45, true);

		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, settings::validated).code());
	}

	@Test
	void rechazaRolesDesconocidos() {
		GameSettings settings = new GameSettings(1, List.of("inventor"), 1, false, 120, 30, 45, true);

		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, settings::validated).code());
	}

	@Test
	void rechazaVidasFueraDeRango() {
		GameSettings negativo = new GameSettings(1, List.of(), -1, false, 120, 30, 45, true);
		GameSettings excesivo = new GameSettings(1, List.of(), 3, false, 120, 30, 45, true);

		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, negativo::validated).code());
		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, excesivo::validated).code());
	}

	@Test
	void rechazaTiemposFueraDeRango() {
		GameSettings debateCorto = new GameSettings(1, List.of(), 1, false, 10, 30, 45, true);
		GameSettings rolLargo = new GameSettings(1, List.of(), 1, false, 120, 500, 45, true);
		GameSettings votoCorto = new GameSettings(1, List.of(), 1, false, 120, 30, 5, true);

		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, debateCorto::validated).code());
		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, rolLargo::validated).code());
		assertEquals(RoomException.INVALID_SETTINGS,
				assertThrows(RoomException.class, votoCorto::validated).code());
	}

	@Test
	void noSePuedeEmpezarConMenosDeCincoJugadores() {
		RoomException exception = assertThrows(RoomException.class,
				() -> GameSettings.DEFAULT.validatedForStart(4));

		assertEquals(RoomException.NOT_ENOUGH_PLAYERS, exception.code());
	}

	@Test
	void elRepartoNoPuedeQuedarseSinAldeanos() {
		GameSettings settings = new GameSettings(2, List.of("seer", "witch", "knight"), 1, false, 120, 30, 45, true);

		RoomException exception = assertThrows(RoomException.class, () -> settings.validatedForStart(5));

		assertEquals(RoomException.INVALID_SETTINGS_FOR_PLAYERS, exception.code());
	}

	@Test
	void lasCartasSiempreSumanLosJugadores() {
		GameSettings settings = new GameSettings(2, List.of("seer", "witch"), 1, false, 120, 30, 45, true);

		assertDoesNotThrow(() -> settings.validatedForStart(8));
		assertEquals(4, settings.villagers(8));
		assertEquals(8, settings.wolves() + settings.roles().size() + settings.villagers(8));
	}
}
