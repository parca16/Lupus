package com.parca16.lupus.protocol;

/**
 * Accion del jugador durante la partida. El campo {@code action} elige el tipo y {@code kind}
 * concreta la variante (por ejemplo, la pocion de la Bruja).
 */
public record GameAction(int v, String id, long at, String action, String kind, String targetId,
		String secondTargetId) implements Message {

	public GameAction {
		Protocol.requireVersion(v);
	}
}
