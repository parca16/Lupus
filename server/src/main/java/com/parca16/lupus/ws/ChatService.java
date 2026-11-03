package com.parca16.lupus.ws;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parca16.lupus.game.ChatChannel;
import com.parca16.lupus.game.ChatPolicy;
import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GamePlayer;
import com.parca16.lupus.game.GameRuleException;
import com.parca16.lupus.protocol.ChatMessage;
import com.parca16.lupus.protocol.ChatSend;
import com.parca16.lupus.protocol.Protocol;

/** Valida y reparte los mensajes de chat segun su canal (#62, #63). */
@Component
public class ChatService {

	static final int MAX_LENGTH = 500;

	private final PlayerSessions sessions;
	private final RoomNotifier notifier;

	public ChatService(PlayerSessions sessions, RoomNotifier notifier) {
		this.sessions = sessions;
		this.notifier = notifier;
	}

	public void post(Game game, String playerId, ChatSend message) {
		ChatChannel channel = ChatChannel.fromWire(message.channel()).orElseThrow(
				() -> new GameRuleException(GameRuleException.INVALID_CHAT, "Canal de chat desconocido."));

		String text = message.text() == null ? "" : message.text().strip();
		if (text.isEmpty() || text.length() > MAX_LENGTH) {
			throw new GameRuleException(GameRuleException.INVALID_CHAT,
					"El mensaje debe tener entre 1 y " + MAX_LENGTH + " caracteres.");
		}

		if (!ChatPolicy.canPost(game, playerId, channel)) {
			throw new GameRuleException(GameRuleException.CHAT_NOT_ALLOWED,
					"No puedes escribir en ese canal en este momento.");
		}

		GamePlayer sender = game.players().stream()
				.filter(player -> player.id().equals(playerId))
				.findFirst()
				.orElseThrow(() -> new GameRuleException(GameRuleException.CHAT_NOT_ALLOWED,
						"No perteneces a esta partida."));

		ChatMessage out = new ChatMessage(Protocol.VERSION, UUID.randomUUID().toString(),
				System.currentTimeMillis(), game.roomCode(), channel.wire(), sender.id(), sender.nickname(),
				text, game.round());

		for (GamePlayer recipient : ChatPolicy.recipients(game, channel)) {
			sessions.find(recipient.id()).ifPresent(session -> notifier.send(session, out));
		}
	}
}
