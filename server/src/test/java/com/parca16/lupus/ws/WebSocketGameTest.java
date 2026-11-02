package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.databind.JsonNode;

/** Partida real por WebSocket: roles secretos, acciones y avance de fases. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "lupus.game.tick-millis=600000")
class WebSocketGameTest {

	@LocalServerPort
	private int port;

	@Autowired
	private GameTicker ticker;

	@Test
	void laPartidaEmpiezaConRolesSecretosYElVidentePuedeActuar() throws Exception {
		try (TestClient host = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(TestMessages.crear("Ana"));
			JsonNode estadoHost = host.nextOfType("room_state");
			String code = estadoHost.get("roomCode").asString();
			host.nextOfType("room_settings");

			Map<String, TestClient> clientes = new LinkedHashMap<>();
			clientes.put(estadoHost.get("playerId").asString(), host);

			List<TestClient> invitados = new ArrayList<>();
			for (int i = 1; i <= 4; i++) {
				TestClient invitado = new TestClient(port);
				invitados.add(invitado);
				invitado.nextOfType("welcome");
				invitado.send(TestMessages.unirse(code, "Jugador" + i));
				JsonNode estado = invitado.nextOfType("room_state");
				invitado.nextOfType("room_settings");
				clientes.put(estado.get("playerId").asString(), invitado);
			}

			host.send(TestMessages.configurar(1, List.of("seer"), 0));
			host.nextOfType("room_settings");

			invitados.get(0).send(TestMessages.empezar());
			assertEquals("not_host", invitados.get(0).nextOfType("error").get("code").asString());

			Instant inicio = Instant.now();
			host.send(TestMessages.empezar());

			Map<String, String> roles = new LinkedHashMap<>();
			for (Map.Entry<String, TestClient> entrada : clientes.entrySet()) {
				entrada.getValue().nextOfType("game_starting");
				JsonNode estado = entrada.getValue().nextOfType("game_state");
				assertEquals(5, estado.get("statuses").size());
				roles.put(entrada.getKey(), estado.get("role").asString());
			}

			assertEquals(1, contar(roles, "WOLF"));
			assertEquals(1, contar(roles, "SEER"));
			assertEquals(3, contar(roles, "VILLAGER"));

			invitados.get(0).send(TestMessages.salir());
			assertEquals("game_already_started",
					invitados.get(0).nextOfType("error").get("code").asString());

			ticker.tick(inicio.plusSeconds(2));
			ticker.tick(inicio.plusSeconds(13));

			String idVidente = roles.entrySet().stream()
					.filter(entrada -> entrada.getValue().equals("SEER"))
					.map(Map.Entry::getKey)
					.findFirst()
					.orElseThrow();
			String idObjetivo = roles.keySet().stream()
					.filter(id -> !id.equals(idVidente))
					.findFirst()
					.orElseThrow();

			TestClient vidente = clientes.get(idVidente);
			vidente.send(TestMessages.accion("night_action", "seer", idObjetivo, ""));

			JsonNode estadoVidente = siguienteVision(vidente);
			assertEquals(roles.get(idObjetivo), estadoVidente.get("vision").asString());
			assertEquals("NIGHT", estadoVidente.get("phase").asString());
			assertEquals("SEER", estadoVidente.get("nightStep").asString());
			assertFalse(estadoVidente.get("role").asString().isEmpty());
		}
	}

	private static JsonNode siguienteVision(TestClient cliente) throws Exception {
		for (int intento = 0; intento < 10; intento++) {
			JsonNode estado = cliente.nextOfType("game_state");
			if (!estado.get("vision").asString().isEmpty()) {
				return estado;
			}
		}
		fail("No llego ningun estado con vision del Vidente");
		return null;
	}

	private static long contar(Map<String, String> roles, String rol) {
		return roles.values().stream().filter(rol::equals).count();
	}
}
