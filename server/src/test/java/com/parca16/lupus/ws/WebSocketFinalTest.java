package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.parca16.lupus.game.GameRegistry;
import com.parca16.lupus.game.Outcome;
import com.parca16.lupus.room.GameState;
import com.parca16.lupus.room.RoomRegistry;

import tools.jackson.databind.JsonNode;

/** Fin de partida: roles revelados y vuelta al lobby con los mismos jugadores (#60). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "lupus.game.tick-millis=600000")
class WebSocketFinalTest {

	@LocalServerPort
	private int port;

	@Autowired
	private GameTicker ticker;

	@Autowired
	private GameRegistry games;

	@Autowired
	private RoomRegistry rooms;

	@Test
	void elFinalRevelaLosRolesYElHostVuelveAlLobby() throws Exception {
		try (TestClient host = new TestClient(port); TestClient invitado = new TestClient(port)) {
			host.nextOfType("welcome");
			host.send(TestMessages.crear("Ana"));
			JsonNode estadoHost = host.nextOfType("room_state");
			String code = estadoHost.get("roomCode").asString();
			host.nextOfType("room_settings");

			invitado.nextOfType("welcome");
			invitado.send(TestMessages.unirse(code, "Luis"));
			invitado.nextOfType("room_state");
			invitado.nextOfType("room_settings");

			invitado.send(TestMessages.volverAlLobby());
			assertEquals("not_host", invitado.nextOfType("error").get("code").asString());

			// Completa la sala minima y arranca; luego forzamos el final para no jugar entera.
			TestClient[] relleno = new TestClient[3];
			for (int i = 0; i < relleno.length; i++) {
				relleno[i] = new TestClient(port);
				relleno[i].nextOfType("welcome");
				relleno[i].send(TestMessages.unirse(code, "Jugador" + (i + 3)));
				relleno[i].nextOfType("room_state");
				relleno[i].nextOfType("room_settings");
			}
			try {
				host.send(TestMessages.empezar());
				host.nextOfType("game_starting");

				games.find(code).orElseThrow().finish(Outcome.LOVERS);
				ticker.tick(Instant.now());

				JsonNode fin = host.nextOfType("game_over");
				assertEquals("LOVERS", fin.get("outcome").asString());
				assertEquals(5, fin.get("roles").size());
				assertEquals(GameState.FINISHED, rooms.find(code).orElseThrow().state());

				host.send(TestMessages.volverAlLobby());

				assertEquals(code, host.nextOfType("returned_to_lobby").get("roomCode").asString());
				assertEquals(code, invitado.nextOfType("returned_to_lobby").get("roomCode").asString());
				assertEquals(GameState.WAITING, rooms.find(code).orElseThrow().state());
			}
			finally {
				for (TestClient cliente : relleno) {
					cliente.close();
				}
			}
		}
	}
}
