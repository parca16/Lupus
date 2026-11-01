package com.parca16.lupus.protocol;

/** Jugador tal y como viaja por el cable. */
public record Player(String id, String nickname, boolean host, boolean connected) {
}
