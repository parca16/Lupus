package com.parca16.lupus.ws;

import java.util.UUID;

import com.parca16.lupus.protocol.CreateRoom;
import com.parca16.lupus.protocol.JoinRoom;
import com.parca16.lupus.protocol.KickPlayer;
import com.parca16.lupus.protocol.LeaveRoom;
import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Protocol;

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

	static Message salir() {
		return new LeaveRoom(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis());
	}

	static Message expulsar(String playerId) {
		return new KickPlayer(Protocol.VERSION, UUID.randomUUID().toString(), System.currentTimeMillis(), playerId);
	}
}
