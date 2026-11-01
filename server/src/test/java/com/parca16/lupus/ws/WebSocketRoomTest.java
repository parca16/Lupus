package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.parca16.lupus.protocol.CreateRoom;
import com.parca16.lupus.protocol.JoinRoom;
import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Protocol;

import tools.jackson.databind.JsonNode;

/** Salas de extremo a extremo: varios clientes WebSocket reales contra el servidor real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketRoomTest {

	@LocalServerPort
	private int port;

	@Test
	void crearSalaDevuelveCodigoDeSeisCaracteresYElCreadorEsHost() throws Exception {
		try (TestClient cliente = new TestClient(port)) {
			cliente.nextOfType("welcome");
			cliente.send(crear("Ana"));

			JsonNode estado = cliente.nextOfType("room_state");

			assertEquals(6, estado.get("roomCode").asString().length());
			assertFalse(estado.get("playerId").asString().isBlank());
			assertEquals(1, estado.get("players").size());
			JsonNode host = estado.get("players").get(0);
			assertTrue(host.get("host").asBoolean());
			assertTrue(host.get("connected").asBoolean());
			assertEquals("Ana", host.get("nickname").asString());
		}
	}

	@Test
	void alEntrarOtroJugadorSeActualizaLaListaDeTodos() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(crear("Ana"));
			String code = host.nextOfType("room_state").get("roomCode").asString();

			invitado.nextOfType("welcome");
			invitado.send(unirse(code, "Luis"));

			JsonNode estadoInvitado = invitado.nextOfType("room_state");
			assertEquals(2, estadoInvitado.get("players").size());

			JsonNode actualizacion = host.nextOfType("room_updated");
			assertEquals(code, actualizacion.get("roomCode").asString());
			assertEquals(2, actualizacion.get("players").size());
			assertFalse(actualizacion.get("players").get(1).get("host").asBoolean());
		}
	}

	@Test
	void elCodigoDeSalaSeAceptaEnMinusculas() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(crear("Ana"));
			String code = host.nextOfType("room_state").get("roomCode").asString();

			invitado.nextOfType("welcome");
			invitado.send(unirse(code.toLowerCase(), "Luis"));

			assertEquals(2, invitado.nextOfType("room_state").get("players").size());
		}
	}

	@Test
	void unApodoDuplicadoDevuelveNicknameTaken() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(crear("Ana"));
			String code = host.nextOfType("room_state").get("roomCode").asString();

			invitado.nextOfType("welcome");
			invitado.send(unirse(code, "ana"));

			JsonNode error = invitado.nextOfType("error");
			assertEquals("nickname_taken", error.get("code").asString());
		}
	}

	@Test
	void unCodigoDesconocidoDevuelveRoomNotFound() throws Exception {
		try (TestClient cliente = new TestClient(port)) {
			cliente.nextOfType("welcome");
			cliente.send(unirse("ZZZZZZ", "Luis"));

			JsonNode error = cliente.nextOfType("error");
			assertEquals("room_not_found", error.get("code").asString());
		}
	}

	@Test
	void alDesconectarseUnJugadorLaListaMuestraQueYaNoEstaConectado() throws Exception {
		try (TestClient host = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(crear("Ana"));
			String code = host.nextOfType("room_state").get("roomCode").asString();

			TestClient invitado = new TestClient(port);
			invitado.nextOfType("welcome");
			invitado.send(unirse(code, "Luis"));
			invitado.nextOfType("room_state");
			host.nextOfType("room_updated");

			invitado.close();

			JsonNode actualizacion = host.nextOfType("room_updated");
			assertEquals(2, actualizacion.get("players").size());
			JsonNode luis = actualizacion.get("players").get(1);
			assertEquals("Luis", luis.get("nickname").asString());
			assertFalse(luis.get("connected").asBoolean());
		}
	}

	private static Message crear(String nickname) {
		return new CreateRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), nickname);
	}

	private static Message unirse(String roomCode, String nickname) {
		return new JoinRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), roomCode,
				nickname);
	}
}
