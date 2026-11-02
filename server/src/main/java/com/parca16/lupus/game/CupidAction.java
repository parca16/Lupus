package com.parca16.lupus.game;

/** El Cupido elige a los dos enamorados la primera noche. */
public record CupidAction(String firstLoverId, String secondLoverId) implements NightAction {

	@Override
	public String targetId() {
		return firstLoverId;
	}
}
