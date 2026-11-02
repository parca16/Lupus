package com.parca16.lupus.game;

/** Voto de un jugador. {@code targetId} nulo significa abstencion. */
public record Vote(String voterId, String targetId) {
}
