package com.parca16.lupus.protocol;

import java.util.Map;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Punto de entrada del protocolo en el servidor: versión, registro de tipos y serialización.
 *
 * <p>El registro {@link #types()} es lo que leen los tests de contrato para comprobar que no
 * falta ningún mensaje respecto a {@code protocol/protocol.json}.
 */
public final class Protocol {

	/** Versión actual del protocolo. Debe coincidir con {@code protocol.json}. */
	public static final int VERSION = 1;

	private static final Map<String, Class<? extends Message>> TYPES = Map.of(
			"hello", Hello.class,
			"welcome", Welcome.class,
			"echo", Echo.class,
			"ping", Ping.class,
			"pong", Pong.class,
			"error", ErrorMessage.class,
			"create_room", CreateRoom.class,
			"join_room", JoinRoom.class,
			"room_state", RoomState.class,
			"room_updated", RoomUpdated.class);

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	private Protocol() {
	}

	public static Map<String, Class<? extends Message>> types() {
		return TYPES;
	}

	/** Vista minima del sobre, util para decidir como tratar un mensaje antes de deserializarlo. */
	public record Envelope(int v, String type, String id) {
	}

	public static Envelope peek(String json) {
		var node = MAPPER.readTree(json);
		return new Envelope(node.path("v").asInt(-1), node.path("type").asString(""),
				node.path("id").asString(""));
	}

	public static String encode(Message message) {
		return MAPPER.writeValueAsString(message);
	}

	public static Message decode(String json) {
		return MAPPER.readValue(json, Message.class);
	}

	public static <T extends Message> T decode(String json, Class<T> type) {
		return MAPPER.readValue(json, type);
	}

	static void requireVersion(int version) {
		if (version != VERSION) {
			throw new IllegalArgumentException("Versión de protocolo no soportada: " + version);
		}
	}
}
