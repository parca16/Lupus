package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.databind.JsonNode;

/** Salir, expulsar, cierre de sala y recuperacion de asiento, con clientes reales. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketLobbyTest {

	@LocalServerPort
	private int port;

	@Test
	void salirDeLaSalaActualizaAlResto() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			String code = crearSala(host);
			unirse(code, "Luis", invitado);
			host.nextOfType("room_updated");

			invitado.send(TestMessages.salir());

			JsonNode actualizacion = host.nextOfType("room_updated");
			assertEquals(1, actualizacion.get("players").size());
			assertEquals("Ana", actualizacion.get("players").get(0).get("nickname").asString());
		}
	}

	@Test
	void siElHostAbandonaElLobbyLaSalaSeCierra() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port);
				TestClient tardio = new TestClient(port)) {
			String code = crearSala(host);
			unirse(code, "Luis", invitado);
			host.nextOfType("room_updated");

			host.send(TestMessages.salir());

			JsonNode cierre = invitado.nextOfType("room_closed");
			assertEquals("host_left", cierre.get("reason").asString());
			assertEquals(code, cierre.get("roomCode").asString());

			tardio.nextOfType("welcome");
			tardio.send(TestMessages.unirse(code, "Tarde"));
			assertEquals("room_not_found", tardio.nextOfType("error").get("code").asString());
		}
	}

	@Test
	void elHostPuedeExpulsarAUnJugador() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			String code = crearSala(host);
			JsonNode estadoInvitado = unirse(code, "Luis", invitado);
			host.nextOfType("room_updated");

			host.send(TestMessages.expulsar(estadoInvitado.get("playerId").asString()));

			assertEquals(code, invitado.nextOfType("kicked").get("roomCode").asString());
			JsonNode actualizacion = host.nextOfType("room_updated");
			assertEquals(1, actualizacion.get("players").size());
		}
	}

	@Test
	void unJugadorQueNoEsHostNoPuedeExpulsar() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			String code = crearSala(host);
			unirse(code, "Luis", invitado);
			host.nextOfType("room_updated");

			invitado.send(TestMessages.expulsar("cualquiera"));

			assertEquals("not_host", invitado.nextOfType("error").get("code").asString());
		}
	}

	@Test
	void siSeCaeLaConexionElJugadorRecuperaSuAsientoAlVolver() throws Exception {
		try (TestClient host = new TestClient(port)) {
			String code = crearSala(host);

			TestClient invitado = new TestClient(port);
			JsonNode estadoInvitado = unirse(code, "Luis", invitado);
			String playerId = estadoInvitado.get("playerId").asString();
			host.nextOfType("room_updated");

			invitado.close();

			JsonNode caida = host.nextOfType("room_updated");
			assertFalse(caida.get("players").get(1).get("connected").asBoolean());

			try (TestClient reconectado = new TestClient(port)) {
				reconectado.nextOfType("welcome");
				reconectado.send(TestMessages.unirse(code, "Luis"));

				JsonNode estado = reconectado.nextOfType("room_state");
				assertEquals(playerId, estado.get("playerId").asString(), "Debe recuperar el mismo asiento");
				assertEquals(2, estado.get("players").size());

				JsonNode vuelta = host.nextOfType("room_updated");
				assertTrue(vuelta.get("players").get(1).get("connected").asBoolean());
			}
		}
	}

	@Test
	void siElHostSeDesconectaLaSalaSigueAbiertaParaQuePuedaVolver() throws Exception {
		try (TestClient invitado = new TestClient(port)) {
			TestClient host = new TestClient(port);
			String code = crearSala(host);
			unirse(code, "Luis", invitado);

			host.close();

			JsonNode caida = invitado.nextOfType("room_updated");
			assertEquals(2, caida.get("players").size());
			assertFalse(caida.get("players").get(0).get("connected").asBoolean());

			try (TestClient hostDeVuelta = new TestClient(port)) {
				hostDeVuelta.nextOfType("welcome");
				hostDeVuelta.send(TestMessages.unirse(code, "Ana"));

				JsonNode estado = hostDeVuelta.nextOfType("room_state");
				assertTrue(estado.get("players").get(0).get("host").asBoolean(),
						"El host conserva el cargo al reconectar");
			}
		}
	}

	private String crearSala(TestClient host) throws Exception {
		host.nextOfType("welcome");
		host.send(TestMessages.crear("Ana"));
		return host.nextOfType("room_state").get("roomCode").asString();
	}

	private JsonNode unirse(String code, String nickname, TestClient cliente) throws Exception {
		cliente.nextOfType("welcome");
		cliente.send(TestMessages.unirse(code, nickname));
		return cliente.nextOfType("room_state");
	}
}
