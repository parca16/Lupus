package com.parca16.lupus.room;

/** Reglas de validacion de apodos compartidas por todas las salas. */
public final class Nicknames {

	public static final int MIN_LENGTH = 3;
	public static final int MAX_LENGTH = 16;

	private Nicknames() {
	}

	/** Devuelve el apodo limpio o lanza {@link RoomException} si no es valido. */
	public static String require(String raw) {
		if (raw == null) {
			throw new RoomException(RoomException.INVALID_NICKNAME, "El apodo es obligatorio.");
		}

		String clean = raw.strip();
		int length = clean.codePointCount(0, clean.length());

		if (length < MIN_LENGTH || length > MAX_LENGTH) {
			throw new RoomException(RoomException.INVALID_NICKNAME,
					"El apodo debe tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres.");
		}

		if (clean.codePoints().anyMatch(Character::isISOControl)) {
			throw new RoomException(RoomException.INVALID_NICKNAME, "El apodo contiene caracteres no permitidos.");
		}

		return clean;
	}
}
