package com.parca16.lupus.ws;

import java.util.List;
import java.util.UUID;

import com.parca16.lupus.protocol.CreateRoom;
import com.parca16.lupus.protocol.GameAction;
import com.parca16.lupus.protocol.JoinRoom;
import com.parca16.lupus.protocol.KickPlayer;
import com.parca16.lupus.protocol.LeaveRoom;
import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.protocol.ResumeRoom;
import com.parca16.lupus.protocol.StartGame;
import com.parca16.lupus.protocol.UpdateSettings;

/** Constructores de mensajes para los tests de integracion. */
final class TestMessages {

	private TestMessages() {
	}

	static Message crear(String nickname) {
		return new CreateRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), nickname);
	}

	static Message unirse(String roomCode, String nickname) {
		return new JoinRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), roomCode,
				nickname);
	}

	static Message reanudar(String roomCode, String playerId) {
		return new ResumeRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), roomCode,
				playerId);
	}

	static Message salir() {
		return new LeaveRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis());
	}

	static Message expulsar(String playerId) {
		return new KickPlayer(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), playerId);
	}

	static Message configurar(int wolves, List<String> roles, int lives) {
		return new UpdateSettings(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				wolves, roles, lives, false, 240, 20, 45, true);
	}

	static Message empezar() {
		return new StartGame(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis());
	}

	static Message accion(String action, String kind, String targetId, String secondTargetId) {
		return new GameAction(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(),
				action, kind, targetId, secondTargetId);
	}
}
