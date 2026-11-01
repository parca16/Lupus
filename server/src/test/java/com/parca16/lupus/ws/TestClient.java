package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Protocol;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Cliente WebSocket de prueba: recibe JSON y permite esperar por tipo de mensaje. */
final class TestClient implements AutoCloseable {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();
	private static final long TIMEOUT_SECONDS = 5;

	private final BlockingQueue<String> received = new LinkedBlockingQueue<>();
	private final WebSocketSession session;

	TestClient(int port) throws Exception {
		this.session = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			@Override
			protected void handleTextMessage(WebSocketSession clientSession, TextMessage message) {
				received.add(message.getPayload());
			}
		}, "ws://localhost:" + port + "/ws").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	void send(Message message) throws Exception {
		session.sendMessage(new TextMessage(Protocol.encode(message)));
	}

	void sendRaw(String json) throws Exception {
		session.sendMessage(new TextMessage(json));
	}

	/** Espera al siguiente mensaje cuyo {@code type} coincida, descartando el resto. */
	JsonNode nextOfType(String type) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);

		while (System.nanoTime() < deadline) {
			String payload = received.poll(200, TimeUnit.MILLISECONDS);
			if (payload == null) {
				continue;
			}
			JsonNode node = MAPPER.readTree(payload);
			if (type.equals(node.get("type").asString())) {
				return node;
			}
		}

		fail("No llego ningun mensaje de tipo '" + type + "'");
		return null;
	}

	JsonNode next() throws Exception {
		String payload = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		assertNotNull(payload, "El servidor no envio ningun mensaje a tiempo");
		return MAPPER.readTree(payload);
	}

	@Override
	public void close() throws Exception {
		if (session.isOpen()) {
			session.close();
		}
	}
}
