package com.parca16.lupus.room;

/** Configuracion de una sala. Los valores definitivos se validan en la issue #27. */
public record RoomSettings(int minPlayers, int maxPlayers) {

	public static final RoomSettings DEFAULT = new RoomSettings(5, 16);

	public RoomSettings {
		if (minPlayers < 2) {
			throw new IllegalArgumentException("La sala necesita al menos 2 jugadores");
		}
		if (maxPlayers < minPlayers) {
			throw new IllegalArgumentException("El aforo maximo no puede ser menor que el minimo");
		}
	}
}
