package com.parca16.lupus.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RoomRegistryTest {

	@Test
	void generaCodigosDeSeisCaracteresSinAmbiguos() {
		RoomRegistry registry = new RoomRegistry(Clock.systemUTC());

		String code = registry.create().code();

		assertEquals(6, code.length());
		assertTrue(code.matches("[A-HJ-NP-Z2-9]{6}"), "Codigo inesperado: " + code);
	}

	@Test
	void noRepiteCodigosEnMuchasSalas() {
		RoomRegistry registry = new RoomRegistry(Clock.systemUTC());
		Set<String> codigos = new HashSet<>();

		for (int i = 0; i < 500; i++) {
			codigos.add(registry.create().code());
		}

		assertEquals(500, codigos.size());
		assertEquals(500, registry.size());
	}

	@Test
	void buscaNormalizandoMayusculasYEspacios() {
		RoomRegistry registry = new RoomRegistry(Clock.systemUTC());
		Room room = registry.create();

		assertEquals(room, registry.find("  " + room.code().toLowerCase() + "  ").orElseThrow());
	}

	@Test
	void eliminaLaSalaPorCodigo() {
		RoomRegistry registry = new RoomRegistry(Clock.systemUTC());
		Room room = registry.create();

		registry.remove(room.code().toLowerCase());

		assertTrue(registry.find(room.code()).isEmpty());
		assertEquals(0, registry.size());
	}

	@Test
	void noEliminaSalasQueSiguenDentroDelPlazo() {
		Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		RoomRegistry registry = new RoomRegistry(clock);
		Room room = registry.create();

		List<Room> expiradas = registry.removeExpired(clock.instant().plus(Duration.ofMinutes(29)),
				Duration.ofMinutes(30));

		assertTrue(expiradas.isEmpty());
		assertTrue(registry.find(room.code()).isPresent());
	}

	@Test
	void eliminaLasSalasCaducadasYLasDevuelve() {
		Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		RoomRegistry registry = new RoomRegistry(clock);
		Room room = registry.create();

		List<Room> expiradas = registry.removeExpired(clock.instant().plus(Duration.ofMinutes(31)),
				Duration.ofMinutes(30));

		assertEquals(List.of(room), expiradas);
		assertTrue(registry.find(room.code()).isEmpty());
		assertEquals(0, registry.size());
	}
}
