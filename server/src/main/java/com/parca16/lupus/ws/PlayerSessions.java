package com.parca16.lupus.ws;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/** Relaciona cada jugador con su conexion WebSocket viva. */
@Component
public class PlayerSessions {

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public void bind(String playerId, WebSocketSession session) {
		sessions.put(playerId, session);
	}

	public void unbind(String playerId) {
		sessions.remove(playerId);
	}

	public Optional<WebSocketSession> find(String playerId) {
		return Optional.ofNullable(sessions.get(playerId));
	}
}
