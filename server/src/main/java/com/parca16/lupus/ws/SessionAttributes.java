package com.parca16.lupus.ws;

import org.springframework.web.socket.WebSocketSession;

/** Claves de los atributos de sesion y utilidades para limpiarlos. */
public final class SessionAttributes {

	public static final String CONNECTION_ID = "connectionId";
	public static final String NICKNAME = "nickname";
	public static final String PLAYER_ID = "playerId";
	public static final String ROOM_CODE = "roomCode";

	private SessionAttributes() {
	}

	/** El jugador deja de pertenecer a una sala, pero la conexion sigue viva. */
	public static void clearMembership(WebSocketSession session) {
		session.getAttributes().remove(PLAYER_ID);
		session.getAttributes().remove(ROOM_CODE);
	}
}
