package com.parca16.lupus.game;

import java.util.List;

/** Resumen del amanecer, listo para narrar (#40). */
public record DawnReport(List<DawnEvent> events) {

	public DawnReport {
		events = List.copyOf(events);
	}

	/** True cuando nadie murio ni perdio vidas: el narrador lo dice explicitamente. */
	public boolean quiet() {
		return events.isEmpty();
	}
}
