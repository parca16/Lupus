package com.parca16.lupus.ws;

import java.util.List;

import com.parca16.lupus.game.Phase;

/**
 * Plantillas del narrador pasivo (#66). Son textos fijos con varias variantes para no repetir
 * siempre la misma frase; no hay IA ni coste.
 */
final class AnnouncementTemplates {

	private AnnouncementTemplates() {
	}

	static String phase(Phase phase, int round) {
		return switch (phase) {
			case PRESIDENT_ELECTION -> pick(List.of(
					"El pueblo elige a su Presidente. Su voto valdrá doble.",
					"Comienza la votación para elegir al Presidente."), round);
			case DEALING -> pick(List.of(
					"El narrador reparte las cartas. Mirad vuestra carta en secreto.",
					"Las cartas están repartidas. Que empiece la noche."), round);
			case REVEAL -> pick(List.of(
					"Tenéis unos segundos para memorizar vuestra carta.",
					"Mirad bien vuestro rol: solo vosotros podéis verlo."), round);
			case NIGHT -> pick(List.of(
					"El pueblo duerme. Los lobos afilan sus colmillos.",
					"Cae la noche. Los roles actúan en silencio.",
					"Silencio: la noche esconde a los que tienen algo que hacer."), round);
			case DAWN -> pick(List.of(
					"Amanece. El pueblo descubre lo que ha pasado esta noche.",
					"El sol vuelve a salir. Es hora de saber quién sigue vivo.",
					"Primera luz del día. El narrador anuncia los sucesos de la noche."), round);
			case KNIGHT_REACTION -> pick(List.of(
					"El Caballero ha caído y elige a quién llevarse consigo.",
					"La espada oxidada busca un último objetivo."), round);
			case DEBATE -> pick(List.of(
					"Comienza el debate. Hablad, acusad y defended vuestras vidas.",
					"El pueblo tiene la palabra. Cuando estéis listos, se votará.",
					"Debatid con cuidado: cada palabra puede salvar o condenar a alguien."), round);
			case VOTING -> pick(List.of(
					"Comienza la votación. Votad en secreto o absteneos.",
					"Es hora de linchar. El voto del Presidente vale doble.",
					"Las sospechas se convierten en votos. Nadie está a salvo."), round);
			case RUNOFF -> pick(List.of(
					"Empate. Segunda vuelta entre los más votados.",
					"Nadie ha ganado todavía: se abre la segunda vuelta."), round);
			case FINISHED -> "La partida ha terminado.";
		};
	}

	static String death(String nickname, int variant) {
		return pick(List.of(
				"%s no ha despertado esta noche.",
				"El cuerpo de %s aparece sin vida al amanecer.",
				"%s ha sido encontrado muerto. El pueblo guarda silencio."), variant)
				.formatted(nickname);
	}

	static String lostLife(String nickname, int livesLeft, int variant) {
		return pick(List.of(
				"%s pierde una vida y sigue en pie. Le quedan %d.",
				"%s ha recibido un golpe, pero sobrevive. Le quedan %d.",
				"%s se tambalea: pierde una vida. Le quedan %d."), variant)
				.formatted(nickname, livesLeft);
	}

	static String healed(String nickname, int variant) {
		return pick(List.of(
				"%s aparece curado al amanecer.",
				"Alguien ha velado por %s: amanece con sus heridas cerradas."), variant)
				.formatted(nickname);
	}

	static String quiet(int variant) {
		return pick(List.of(
				"La noche ha sido tranquila. Nadie ha muerto.",
				"El pueblo amanece intacto: esta noche no ha habido víctimas.",
				"Ni un solo cuerpo esta mañana. La noche ha pasado de largo."), variant);
	}

	private static String pick(List<String> options, int seed) {
		return options.get(Math.floorMod(seed, options.size()));
	}
}
