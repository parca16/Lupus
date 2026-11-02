package com.parca16.lupus.game;

/** El Angel de la Guarda protege a un jugador durante la noche. */
public record GuardianAction(String targetId) implements NightAction {
}
