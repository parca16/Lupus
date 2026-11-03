package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.game.Phase;

/** Plantillas del narrador pasivo: textos y variantes (#66). */
class AnnouncementTemplatesTest {

	@Test
	void todasLasFasesTienenAnuncio() {
		for (Phase phase : Phase.values()) {
			assertFalse(AnnouncementTemplates.phase(phase, 1).isBlank(), "Falta anuncio para " + phase);
		}
	}

	@Test
	void lasFasesNoRepitenSiempreLaMismaFrase() {
		assertNotEquals(AnnouncementTemplates.phase(Phase.NIGHT, 1),
				AnnouncementTemplates.phase(Phase.NIGHT, 2));
		assertNotEquals(AnnouncementTemplates.phase(Phase.VOTING, 1),
				AnnouncementTemplates.phase(Phase.VOTING, 2));
	}

	@Test
	void losSucesosIncluyenElNombre() {
		assertTrue(AnnouncementTemplates.death("Ana", 0).contains("Ana"));
		assertTrue(AnnouncementTemplates.lostLife("Luis", 1, 0).contains("Luis"));
		assertTrue(AnnouncementTemplates.lostLife("Luis", 1, 0).contains("1"));
		assertTrue(AnnouncementTemplates.healed("Marta", 0).contains("Marta"));
	}

	@Test
	void laNocheTranquilaTieneVariantes() {
		assertNotEquals(AnnouncementTemplates.quiet(0), AnnouncementTemplates.quiet(1));
	}
}
