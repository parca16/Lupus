package com.parca16.lupus.game;

import java.util.List;
import java.util.Map;

/**
 * Recuento de una votacion.
 *
 * <p>Los votos son secretos hasta este momento. Si la sala no tiene activado el detalle, la lista
 * {@code details} llega vacia y solo se publican los totales.
 */
public record VoteResult(String lynchedId, boolean runoffNeeded, List<String> tied,
		Map<String, Integer> counts, List<Vote> details, int abstentions) {

	public VoteResult {
		tied = List.copyOf(tied);
		counts = Map.copyOf(counts);
		details = List.copyOf(details);
	}
}
