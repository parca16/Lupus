package com.parca16.lupus.protocol;

/** Anuncio del narrador pasivo: texto plantillado y fiable, sin IA (#66). */
public record GameAnnouncement(int v, String id, long at, String roomCode, int round, String kind, String text)
		implements Message {

	public GameAnnouncement {
		Protocol.requireVersion(v);
	}
}
