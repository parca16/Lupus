package com.parca16.lupus.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RoomRegistryTest {

	@Test
	void generaCodigosDeSeisCaracteresSinAmbiguos() {
		RoomRegistry registry = new RoomRegistry();

		String code = registry.create().code();

		assertEquals(6, code.length());
		assertTrue(code.matches("[A-HJ-NP-Z2-9]{6}"), "Codigo inesperado: " + code);
	}

	@Test
	void noRepiteCodigosEnMuchasSalas() {
		RoomRegistry registry = new RoomRegistry();
		Set<String> codigos = new HashSet<>();

		for (int i = 0; i < 500; i++) {
			codigos.add(registry.create().code());
		}

		assertEquals(500, codigos.size());
		assertEquals(500, registry.size());
	}

	@Test
	void buscaNormalizandoMayusculasYEspacios() {
		RoomRegistry registry = new RoomRegistry();
		Room room = registry.create();

		assertEquals(room, registry.find("  " + room.code().toLowerCase() + "  ").orElseThrow());
	}

	@Test
	void eliminaLaSalaPorCodigo() {
		RoomRegistry registry = new RoomRegistry();
		Room room = registry.create();

		registry.remove(room.code().toLowerCase());

		assertTrue(registry.find(room.code()).isEmpty());
		assertEquals(0, registry.size());
	}
}
