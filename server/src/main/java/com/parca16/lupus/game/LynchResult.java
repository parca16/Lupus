package com.parca16.lupus.game;

/**
 * Resultado del linchamiento.
 *
 * <p>El rol solo se revela si el jugador muere de forma definitiva (D7).
 */
public record LynchResult(String targetId, boolean survived, boolean died, Role revealedRole,
		HitResult hitResult) {
}
