package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.parca16.lupus.protocol.Protocol;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Prueba de extremo a extremo: servidor Spring real + cliente WebSocket real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketEchoTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	@LocalServerPort
	private int port;

	private final BlockingQueue<String> received = new LinkedBlockingQueue<>();

	private WebSocketSession session;

	@BeforeEach
	void conectar() throws Exception {
		session = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			@Override
			protected void handleTextMessage(WebSocketSession clientSession, TextMessage message) {
				received.add(message.getPayload());
			}
		}, "ws://localhost:" + port + "/ws").get(5, TimeUnit.SECONDS);
	}

	@AfterEach
	void desconectar() throws Exception {
		if (session != null && session.isOpen()) {
			session.close();
		}
	}

	@Test
	void elHandshakeEntregaUnIdentificadorDeConexionYConfirmaElApodo() throws Exception {
		JsonNode welcome = siguienteMensaje();

		assertEquals("welcome", welcome.get("type").asString());
		assertFalse(welcome.get("connectionId").asString().isBlank(),
				"El welcome debe traer un identificador de conexion");

		enviar("{\"v\":1,\"type\":\"hello\",\"id\":\"hello-1\",\"at\":1,\"nickname\":\"Ana\"}");
		JsonNode trasHello = siguienteMensaje();

		assertEquals("welcome", trasHello.get("type").asString());
		assertEquals("Ana", trasHello.get("nickname").asString());
		assertEquals(welcome.get("connectionId").asString(), trasHello.get("connectionId").asString(),
				"El identificador no debe cambiar tras el hello");
	}

	@Test
	void elEchoDevuelveElMismoTexto() throws Exception {
		siguienteMensaje(); // welcome

		enviar("{\"v\":1,\"type\":\"echo\",\"id\":\"echo-1\",\"at\":10,\"text\":\"hola\"}");
		JsonNode echo = siguienteMensaje();

		assertEquals("echo", echo.get("type").asString());
		assertEquals("echo-1", echo.get("id").asString());
		assertEquals("hola", echo.get("text").asString());
	}

	@Test
	void pingRespondeConPong() throws Exception {
		siguienteMensaje(); // welcome

		enviar("{\"v\":1,\"type\":\"ping\",\"id\":\"ping-1\",\"at\":20}");
		JsonNode pong = siguienteMensaje();

		assertEquals("pong", pong.get("type").asString());
		assertEquals("ping-1", pong.get("id").asString());
		assertTrue(pong.get("at").asLong() > 0);
	}

	@Test
	void unMensajeQueNoEsJsonDevuelveInvalidMessage() throws Exception {
		siguienteMensaje(); // welcome

		enviar("esto no es json");

		assertEquals("invalid_message", siguienteError());
	}

	@Test
	void unTipoDesconocidoDevuelveUnknownType() throws Exception {
		siguienteMensaje(); // welcome

		enviar("{\"v\":1,\"type\":\"no-existe\",\"id\":\"x\",\"at\":1}");

		assertEquals("unknown_type", siguienteError());
	}

	@Test
	void unaVersionNoSoportadaDevuelveUnsupportedVersion() throws Exception {
		siguienteMensaje(); // welcome

		enviar("{\"v\":99,\"type\":\"ping\",\"id\":\"x\",\"at\":1}");

		assertEquals("unsupported_version", siguienteError());
	}

	private void enviar(String json) throws Exception {
		session.sendMessage(new TextMessage(json));
	}

	private JsonNode siguienteMensaje() throws Exception {
		String payload = received.poll(5, TimeUnit.SECONDS);
		assertNotNull(payload, "El servidor no envio ningun mensaje a tiempo");
		JsonNode node = MAPPER.readTree(payload);
		assertEquals(Protocol.VERSION, node.get("v").asInt(), "Version inesperada en " + payload);
		return node;
	}

	private String siguienteError() throws Exception {
		JsonNode error = siguienteMensaje();
		assertEquals("error", error.get("type").asString(), "Se esperaba un mensaje error");
		assertFalse(error.get("message").asString().isBlank());
		return error.get("code").asString();
	}
}
