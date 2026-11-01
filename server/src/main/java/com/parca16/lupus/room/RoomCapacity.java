package com.parca16.lupus.room;

/** Aforo de una sala. Los valores definitivos se validan en la issue #27. */
public record RoomCapacity(int minPlayers, int maxPlayers) {

	public static final RoomCapacity DEFAULT = new RoomCapacity(5, 16);

	public RoomCapacity {
		if (minPlayers < 2) {
			throw new IllegalArgumentException("La sala necesita al menos 2 jugadores");
		}
		if (maxPlayers < minPlayers) {
			throw new IllegalArgumentException("El aforo maximo no puede ser menor que el minimo");
		}
	}
}
