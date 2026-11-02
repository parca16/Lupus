package com.parca16.lupus.room;

/** Error de negocio de una sala; el codigo viaja al cliente en un mensaje {@code error}. */
public class RoomException extends RuntimeException {

	public static final String INVALID_NICKNAME = "invalid_nickname";
	public static final String ROOM_NOT_FOUND = "room_not_found";
	public static final String ROOM_FULL = "room_full";
	public static final String NICKNAME_TAKEN = "nickname_taken";
	public static final String NOT_IN_ROOM = "not_in_room";
	public static final String NOT_HOST = "not_host";
	public static final String INVALID_SETTINGS = "invalid_settings";
	public static final String NOT_ENOUGH_PLAYERS = "not_enough_players";
	public static final String INVALID_SETTINGS_FOR_PLAYERS = "invalid_settings_for_players";
	public static final String GAME_ALREADY_STARTED = "game_already_started";
	public static final String SEAT_NOT_FOUND = "seat_not_found";
	public static final String ALREADY_CONNECTED = "already_connected";

	private final String code;

	public RoomException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
