package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Caducidad de salas sin actividad (issue #25) con tiempos muy cortos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"lupus.rooms.expiration-millis=400",
		"lupus.rooms.sweep-millis=100"
})
class WebSocketRoomExpiryTest {

	@LocalServerPort
	private int port;

	@Test
	void unaSalaSinActividadSeCierraYSeAvisa() throws Exception {
		try (TestClient host = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(TestMessages.crear("Ana"));
			String code = host.nextOfType("room_state").get("roomCode").asString();
			host.nextOfType("room_settings");

			var cierre = host.nextOfType("room_closed");

			assertEquals("inactivity", cierre.get("reason").asString());
			assertEquals(code, cierre.get("roomCode").asString());
		}
	}

	@Test
	void unaSalaCaducadaYaNoSePuedeUsar() throws Exception {
		try (TestClient host = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(TestMessages.crear("Ana"));
			String code = host.nextOfType("room_state").get("roomCode").asString();
			host.nextOfType("room_closed");

			host.send(TestMessages.unirse(code, "Luis"));

			assertEquals("room_not_found", host.nextOfType("error").get("code").asString());
		}
	}
}
