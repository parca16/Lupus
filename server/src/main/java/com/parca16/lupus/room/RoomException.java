package com.parca16.lupus.room;

/** Error de negocio de una sala; el codigo viaja al cliente en un mensaje {@code error}. */
public class RoomException extends RuntimeException {

	public static final String INVALID_NICKNAME = "invalid_nickname";
	public static final String ROOM_NOT_FOUND = "room_not_found";
	public static final String ROOM_FULL = "room_full";
	public static final String NICKNAME_TAKEN = "nickname_taken";
	public static final String NOT_IN_ROOM = "not_in_room";

	private final String code;

	public RoomException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
