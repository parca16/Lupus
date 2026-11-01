package com.parca16.lupus.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test de contrato: mantiene sincronizados {@code protocol/protocol.json}, las clases Java
 * espejo y (con su equivalente en el cliente) los tipos TypeScript.
 *
 * <p>Si añades un mensaje solo en un lado, este test falla.
 */
class ProtocolContractTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();
	private static final Set<String> ENVELOPE_FIELDS = Set.of("v", "type", "id", "at");

	@Test
	void elContratoYElRegistroDeclaranLosMismosMensajes() {
		assertEquals(fieldNames(contract().get("messages")), new TreeSet<>(Protocol.types().keySet()),
				"El registro del servidor y protocol/protocol.json deben declarar los mismos mensajes");
	}

	@Test
	void laVersionDelContratoCoincideConLaDelCodigo() {
		assertEquals(Protocol.VERSION, contract().get("version").asInt(),
				"Sube Protocol.VERSION y protocol.json a la vez");
	}

	@Test
	void cadaEjemploSeDeserializaYVuelveASerializarIgual() {
		JsonNode messages = contract().get("messages");
		Map<String, String> fallos = new LinkedHashMap<>();

		for (String type : fieldNames(messages)) {
			JsonNode example = messages.get(type).get("example");
			Class<? extends Message> target = Protocol.types().get(type);
			Message decoded = Protocol.decode(example.toString(), target);
			JsonNode roundtrip = MAPPER.readTree(Protocol.encode(decoded));

			if (!example.equals(roundtrip)) {
				fallos.put(type, roundtrip.toPrettyString());
			}
		}

		assertTrue(fallos.isEmpty(), () -> "Ejemplos que no sobreviven al roundtrip: " + fallos);
	}

	@Test
	void losCamposDelContratoCoincidenConLosRecords() {
		JsonNode messages = contract().get("messages");

		for (String type : fieldNames(messages)) {
			Class<? extends Message> target = Protocol.types().get(type);
			Map<String, String> esperados = kinds(messages.get(type).get("fields"));
			Map<String, String> reales = kindsDeLosRecords(target);

			assertEquals(esperados, reales,
					() -> "Los campos de '" + type + "' no coinciden con " + target.getSimpleName());
		}
	}

	@Test
	void seRechazaUnTipoDesconocido() {
		String json = "{\"v\":1,\"type\":\"no-existe\",\"id\":\"x\",\"at\":1}";
		assertThrows(RuntimeException.class, () -> Protocol.decode(json));
	}

	@Test
	void seRechazaUnaVersionNoSoportada() {
		String json = "{\"v\":99,\"type\":\"ping\",\"id\":\"x\",\"at\":1}";
		assertThrows(RuntimeException.class, () -> Protocol.decode(json));
	}

	private static JsonNode contract() {
		Path path = Path.of("..", "protocol", "protocol.json");
		if (!Files.exists(path)) {
			path = Path.of("protocol", "protocol.json");
		}
		try {
			return MAPPER.readTree(Files.readString(path));
		}
		catch (Exception exception) {
			throw new IllegalStateException("No puedo leer el contrato en " + path.toAbsolutePath(), exception);
		}
	}

	private static Set<String> fieldNames(JsonNode node) {
		Set<String> names = new TreeSet<>();
		node.propertyNames().forEach(names::add);
		return names;
	}

	private static Map<String, String> kinds(JsonNode fields) {
		Map<String, String> result = new TreeMap<>();
		for (String name : fieldNames(fields)) {
			result.put(name, fields.get(name).asString());
		}
		return result;
	}

	private static Map<String, String> kindsDeLosRecords(Class<? extends Message> target) {
		Map<String, String> result = new TreeMap<>();
		Arrays.stream(target.getRecordComponents())
				.filter(component -> !ENVELOPE_FIELDS.contains(component.getName()))
				.forEach(component -> result.put(component.getName(), tipoJson(component)));
		return result;
	}

	private static String tipoJson(RecordComponent component) {
		Class<?> type = component.getType();
		if (type == String.class) {
			return "string";
		}
		if (type == int.class || type == long.class || type == double.class) {
			return "number";
		}
		if (type == boolean.class) {
			return "boolean";
		}
		if (java.util.List.class.isAssignableFrom(type)) {
			return "array";
		}
		return type.getSimpleName();
	}
}
