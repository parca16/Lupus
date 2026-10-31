package com.parca16.lupus.protocol;

/** Mensaje de prueba: el servidor devuelve el mismo texto. */
public record Echo(int v, String id, long at, String text) implements Message {

	public Echo {
		Protocol.requireVersion(v);
	}
}
