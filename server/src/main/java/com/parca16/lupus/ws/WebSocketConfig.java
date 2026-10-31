package com.parca16.lupus.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Expone el endpoint WebSocket del juego en {@code /ws}.
 *
 * <p>Los origenes estan abiertos para desarrollo; el endurecimiento por dominio llega con la
 * issue #73 (dominio, HTTPS y WSS).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final LupusWebSocketHandler handler;

	public WebSocketConfig(LupusWebSocketHandler handler) {
		this.handler = handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(handler, "/ws").setAllowedOriginPatterns("*");
	}
}
