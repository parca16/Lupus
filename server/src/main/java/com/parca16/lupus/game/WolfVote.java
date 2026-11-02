package com.parca16.lupus.game;

/** Voto interno de un lobo. Puede cambiar mientras dure su ventana. */
public record WolfVote(String targetId) implements NightAction {
}
