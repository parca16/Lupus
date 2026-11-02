package com.parca16.lupus.protocol;

/** Estado publico de un jugador durante la partida. */
public record GameStatus(String id, String nickname, int lives, boolean alive, boolean president) {
}
