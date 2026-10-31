package com.parca16.lupus.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Mensaje del protocolo Lupus.
 *
 * <p>La forma canónica está en {@code protocol/protocol.json}; esta jerarquía sellada es su
 * espejo en Java. El discriminante {@code type} viaja en el mismo nivel que el resto de campos.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = Hello.class, name = "hello"),
		@JsonSubTypes.Type(value = Welcome.class, name = "welcome"),
		@JsonSubTypes.Type(value = Echo.class, name = "echo"),
		@JsonSubTypes.Type(value = Ping.class, name = "ping"),
		@JsonSubTypes.Type(value = Pong.class, name = "pong"),
		@JsonSubTypes.Type(value = ErrorMessage.class, name = "error")
})
public sealed interface Message permits Hello, Welcome, Echo, Ping, Pong, ErrorMessage {
}
