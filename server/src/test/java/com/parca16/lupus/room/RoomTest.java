package com.parca16.lupus.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoomTest {

	private static final String CODE = "ABC234";

	private Room nuevaSala() {
		return new Room(CODE, RoomSettings.DEFAULT);
	}

	@Test
	void laSalaNuevaEstaEsperandoYSinJugadores() {
		Room room = nuevaSala();

		assertEquals(CODE, room.code());
		assertEquals(GameState.WAITING, room.state());
		assertEquals(0, room.size());
		assertEquals(RoomSettings.DEFAULT, room.settings());
	}

	@Test
	void elPrimerJugadorEsElHostYElSegundoNo() {
		Room room = nuevaSala();

		RoomPlayer host = room.addPlayer("Ana");
		RoomPlayer invitado = room.addPlayer("Luis");

		assertTrue(host.host());
		assertTrue(host.connected());
		assertFalse(invitado.host());
		assertEquals(2, room.size());
	}

	@Test
	void noAdmiteApodosDuplicadosIgnorandoMayusculas() {
		Room room = nuevaSala();
		room.addPlayer("Ana");

		RoomException exception = assertThrows(RoomException.class, () -> room.addPlayer("ana"));

		assertEquals(RoomException.NICKNAME_TAKEN, exception.code());
	}

	@Test
	void elAforoMaximoEsDeDieciseisJugadores() {
		Room room = nuevaSala();
		for (int i = 1; i <= 16; i++) {
			room.addPlayer("Jugador" + i);
		}

		RoomException exception = assertThrows(RoomException.class, () -> room.addPlayer("Sobrante"));

		assertEquals(RoomException.ROOM_FULL, exception.code());
	}

	@Test
	void desconectarMarcaAlJugadorSinSacarloDeLaSala() {
		Room room = nuevaSala();
		RoomPlayer host = room.addPlayer("Ana");

		RoomPlayer desconectado = room.disconnect(host.id());

		assertFalse(desconectado.connected());
		assertTrue(room.contains(host.id()));
		assertEquals(1, room.size());
		assertTrue(room.players().get(0).host(), "El host conserva el cargo mientras este en la sala");
	}

	@Test
	void desconectarAAlguienQueNoEstaEnLaSalaFalla() {
		Room room = nuevaSala();

		RoomException exception = assertThrows(RoomException.class, () -> room.disconnect("fantasma"));

		assertEquals(RoomException.NOT_IN_ROOM, exception.code());
	}

	@Test
	void elApodoSeLimpiaYDebeTenerEntreTresYDieciseisCaracteres() {
		Room room = nuevaSala();

		assertEquals("Ana", room.addPlayer("  Ana  ").nickname());
		assertEquals(RoomException.INVALID_NICKNAME,
				assertThrows(RoomException.class, () -> room.addPlayer("ab")).code());
		assertEquals(RoomException.INVALID_NICKNAME,
				assertThrows(RoomException.class, () -> room.addPlayer("a".repeat(17))).code());
		assertEquals(RoomException.INVALID_NICKNAME,
				assertThrows(RoomException.class, () -> room.addPlayer(null)).code());
	}
}
