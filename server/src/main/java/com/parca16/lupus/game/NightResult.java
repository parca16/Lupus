package com.parca16.lupus.game;

import java.util.List;

/** Resultado de una noche, listo para narrar en el amanecer (#40). */
public record NightResult(String wolfTargetId, boolean wolfBlocked, List<NightEvent> events) {

	public NightResult {
		events = List.copyOf(events);
	}
}
