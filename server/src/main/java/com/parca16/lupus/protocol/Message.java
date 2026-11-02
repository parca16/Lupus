package com.parca16.lupus.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Mensaje del protocolo Lupus.
 *
 * <p>La forma canónica está en {@code protocol/protocol.json}; esta jerarquía sellada es su
 * espejo en Java. El discriminante {@code type} viaja en el mismo nivel que el resto de campos.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = Hello.class, name = "hello"),
		@JsonSubTypes.Type(value = Welcome.class, name = "welcome"),
		@JsonSubTypes.Type(value = Echo.class, name = "echo"),
		@JsonSubTypes.Type(value = Ping.class, name = "ping"),
		@JsonSubTypes.Type(value = Pong.class, name = "pong"),
		@JsonSubTypes.Type(value = ErrorMessage.class, name = "error"),
		@JsonSubTypes.Type(value = CreateRoom.class, name = "create_room"),
		@JsonSubTypes.Type(value = JoinRoom.class, name = "join_room"),
		@JsonSubTypes.Type(value = ResumeRoom.class, name = "resume_room"),
		@JsonSubTypes.Type(value = RoomState.class, name = "room_state"),
		@JsonSubTypes.Type(value = RoomUpdated.class, name = "room_updated"),
		@JsonSubTypes.Type(value = LeaveRoom.class, name = "leave_room"),
		@JsonSubTypes.Type(value = KickPlayer.class, name = "kick_player"),
		@JsonSubTypes.Type(value = RoomClosed.class, name = "room_closed"),
		@JsonSubTypes.Type(value = Kicked.class, name = "kicked"),
		@JsonSubTypes.Type(value = UpdateSettings.class, name = "update_settings"),
		@JsonSubTypes.Type(value = RoomSettings.class, name = "room_settings"),
		@JsonSubTypes.Type(value = StartGame.class, name = "start_game"),
		@JsonSubTypes.Type(value = GameStarting.class, name = "game_starting")
})
public sealed interface Message permits Hello, Welcome, Echo, Ping, Pong, ErrorMessage, CreateRoom, JoinRoom,
		ResumeRoom, RoomState, RoomUpdated, LeaveRoom, KickPlayer, RoomClosed, Kicked, UpdateSettings, RoomSettings,
		StartGame, GameStarting {
}
