package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.parca16.lupus.game.Game;
import com.parca16.lupus.game.GameParticipant;
import com.parca16.lupus.game.GameRuleException;
import com.parca16.lupus.game.Phase;
import com.parca16.lupus.protocol.ChatSend;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.room.GameSettings;
import com.parca16.lupus.room.RoomRegistry;

/** Validacion de los mensajes de chat (#62). */
class ChatServiceTest {

	private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	private final PlayerSessions sessions = new PlayerSessions();
	private final ChatService chat = new ChatService(sessions,
			new RoomNotifier(sessions, new RoomRegistry(CLOCK)));

	@Test
	void aceptaUnMensajePublicoDuranteElDebate() {
		Game game = enDebate();

		assertDoesNotThrow(() -> chat.post(game, "p1", mensaje("public", "Hola a todos")));
	}

	@Test
	void rechazaCanalesDesconocidosYTextosVacios() {
		Game game = enDebate();

		assertEquals(GameRuleException.INVALID_CHAT, assertThrows(GameRuleException.class,
				() -> chat.post(game, "p1", mensaje("inventado", "Hola"))).code());
		assertEquals(GameRuleException.INVALID_CHAT, assertThrows(GameRuleException.class,
				() -> chat.post(game, "p1", mensaje("public", "   "))).code());
		assertEquals(GameRuleException.INVALID_CHAT, assertThrows(GameRuleException.class,
				() -> chat.post(game, "p1", mensaje("public", "x".repeat(501)))).code());
	}

	@Test
	void rechazaElCanalSiLaFaseNoLoPermite() {
		Game game = enDebate();

		assertEquals(GameRuleException.CHAT_NOT_ALLOWED, assertThrows(GameRuleException.class,
				() -> chat.post(game, "p1", mensaje("wolves", "Hola"))).code());
	}

	private static ChatSend mensaje(String channel, String text) {
		return new ChatSend(Protocol.VERSION, "m-1", 1L, channel, text);
	}

	private static Game enDebate() {
		GameSettings settings = new GameSettings(1, List.of(), 1, false, 240, 20, 45, true);
		Game game = Game.start("ABC234", settings, participantes(), CLOCK, new Random(7));
		for (int i = 0; i < 20 && game.phase() != Phase.DEBATE; i++) {
			game.advance(T0);
		}
		assertEquals(Phase.DEBATE, game.phase());
		return game;
	}

	private static List<GameParticipant> participantes() {
		return IntStream.rangeClosed(1, 6)
				.mapToObj(i -> new GameParticipant("p" + i, "Jugador" + i))
				.toList();
	}
}
