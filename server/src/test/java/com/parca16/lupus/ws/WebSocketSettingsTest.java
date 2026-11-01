package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.protocol.StartGame;
import com.parca16.lupus.protocol.UpdateSettings;

import tools.jackson.databind.JsonNode;

/** Configuracion de partida y arranque, con clientes reales. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketSettingsTest {

	@LocalServerPort
	private int port;

	@Test
	void alCrearLaSalaLleganLosAjustesPorDefecto() throws Exception {
		try (TestClient host = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(TestMessages.crear("Ana"));
			host.nextOfType("room_state");

			JsonNode ajustes = host.nextOfType("room_settings");

			assertEquals(1, ajustes.get("wolves").asInt());
			assertEquals(0, ajustes.get("roles").size());
			assertEquals(1, ajustes.get("lives").asInt());
			assertFalse(ajustes.get("president").asBoolean());
			assertEquals(120, ajustes.get("debateSeconds").asInt());
			assertEquals(30, ajustes.get("roleSeconds").asInt());
			assertEquals(45, ajustes.get("voteSeconds").asInt());
			assertTrue(ajustes.get("showVoteDetail").asBoolean());
		}
	}

	@Test
	void elHostCambiaLaConfiguracionYTodosLaReciben() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			String code = crearSala(host);
			entrar(invitado, code, "Luis");

			host.send(configuracion(2, List.of("seer", "witch"), 2));

			JsonNode enHost = host.nextOfType("room_settings");
			JsonNode enInvitado = invitado.nextOfType("room_settings");

			assertEquals(2, enHost.get("wolves").asInt());
			assertEquals(2, enHost.get("lives").asInt());
			assertEquals(2, enHost.get("roles").size());
			assertEquals("seer", enHost.get("roles").get(0).asString());
			assertEquals(2, enInvitado.get("wolves").asInt());
		}
	}

	@Test
	void unJugadorQueNoEsHostNoPuedeCambiarLaConfiguracion() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			String code = crearSala(host);
			entrar(invitado, code, "Luis");

			invitado.send(configuracion(3, List.of(), 1));

			assertEquals("not_host", invitado.nextOfType("error").get("code").asString());
		}
	}

	@Test
	void unaConfiguracionInvalidaSeRechazaConMensajeClaro() throws Exception {
		try (TestClient host = new TestClient(port)) {
			crearSala(host);

			host.send(configuracion(1, List.of(), 3));

			JsonNode error = host.nextOfType("error");
			assertEquals("invalid_settings", error.get("code").asString());
			assertFalse(error.get("message").asString().isBlank());
		}
	}

	@Test
	void noSePuedeEmpezarConMenosDeCincoJugadores() throws Exception {
		try (TestClient host = new TestClient(port)) {
			crearSala(host);

			host.send(empezar());

			JsonNode error = host.nextOfType("error");
			assertEquals("not_enough_players", error.get("code").asString());
			assertTrue(error.get("message").asString().contains("5"));
		}
	}

	@Test
	void unJugadorQueNoEsHostNoPuedeEmpezar() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			String code = crearSala(host);
			entrar(invitado, code, "Luis");

			invitado.send(empezar());

			assertEquals("not_host", invitado.nextOfType("error").get("code").asString());
		}
	}

	@Test
	void elHostEmpiezaLaPartidaConCincoJugadores() throws Exception {
		List<TestClient> invitados = new ArrayList<>();

		try (TestClient host = new TestClient(port)) {
			String code = crearSala(host);

			for (int i = 1; i <= 4; i++) {
				TestClient invitado = new TestClient(port);
				invitados.add(invitado);
				entrar(invitado, code, "Jugador" + i);
			}

			host.send(empezar());

			assertEquals(code, host.nextOfType("game_starting").get("roomCode").asString());
			for (TestClient invitado : invitados) {
				assertEquals(code, invitado.nextOfType("game_starting").get("roomCode").asString());
			}
		}
		finally {
			for (TestClient invitado : invitados) {
				invitado.close();
			}
		}
	}

	@Test
	void noSePuedeEntrarEnUnaPartidaYaEmpezada() throws Exception {
		List<TestClient> invitados = new ArrayList<>();

		try (TestClient host = new TestClient(port); TestClient tardio = new TestClient(port)) {
			String code = crearSala(host);

			for (int i = 1; i <= 4; i++) {
				TestClient invitado = new TestClient(port);
				invitados.add(invitado);
				entrar(invitado, code, "Jugador" + i);
			}

			host.send(empezar());
			host.nextOfType("game_starting");

			tardio.nextOfType("welcome");
			tardio.send(TestMessages.unirse(code, "Tarde"));

			assertEquals("game_already_started", tardio.nextOfType("error").get("code").asString());
		}
		finally {
			for (TestClient invitado : invitados) {
				invitado.close();
			}
		}
	}

	private String crearSala(TestClient host) throws Exception {
		host.nextOfType("welcome");
		host.send(TestMessages.crear("Ana"));
		String code = host.nextOfType("room_state").get("roomCode").asString();
		host.nextOfType("room_settings");
		return code;
	}

	private void entrar(TestClient cliente, String code, String nickname) throws Exception {
		cliente.nextOfType("welcome");
		cliente.send(TestMessages.unirse(code, nickname));
		cliente.nextOfType("room_state");
		cliente.nextOfType("room_settings");
	}

	private static Message configuracion(int wolves, List<String> roles, int lives) {
		return new UpdateSettings(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				wolves, roles, lives, false, 120, 30, 45, true);
	}

	private static Message empezar() {
		return new StartGame(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis());
	}
}
