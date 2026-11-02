package com.parca16.lupus.protocol;

/** Votos ponderados de un jugador en el recuento de una votacion. */
public record GameVoteCount(String playerId, int votes) {
}
