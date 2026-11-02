package com.parca16.lupus.game;

/** La Bruja cura, envenena o pasa. */
public record WitchAction(WitchPotion potion, String targetId) implements NightAction {
}
