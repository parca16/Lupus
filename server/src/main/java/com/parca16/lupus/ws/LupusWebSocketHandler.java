package com.parca16.lupus.ws;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.parca16.lupus.protocol.Echo;
import com.parca16.lupus.protocol.ErrorMessage;
import com.parca16.lupus.protocol.Hello;
import com.parca16.lupus.protocol.Message;
import com.parca16.lupus.protocol.Ping;
import com.parca16.lupus.protocol.Pong;
import com.parca16.lupus.protocol.Protocol;
import com.parca16.lupus.protocol.Welcome;

/**
 * Handshake y primer eco del servidor.
 *
 * <p>Al abrir la conexion el servidor manda un {@code welcome} con el identificador de conexion.
 * Cuando el cliente envia {@code hello}, el servidor repite el {@code welcome} ya con el apodo.
 */
@Component
public class LupusWebSocketHandler extends TextWebSocketHandler {

	static final String CONNECTION_ID = "connectionId";
	static final String NICKNAME = "nickname";

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		session.getAttributes().put(CONNECTION_ID, UUID.randomUUID().toString());
		session.getAttributes().put(NICKNAME, "");
		sendWelcome(session);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		Protocol.Envelope envelope;
		try {
			envelope = Protocol.peek(message.getPayload());
		}
		catch (RuntimeException exception) {
			sendError(session, "invalid_message", "El mensaje no es JSON valido.");
			return;
		}

		if (envelope.v() != Protocol.VERSION) {
			sendError(session, "unsupported_version",
					"Version de protocolo no soportada: " + envelope.v());
			return;
		}

		Class<? extends Message> target = Protocol.types().get(envelope.type());
		if (target == null) {
			sendError(session, "unknown_type", "Tipo de mensaje desconocido: " + envelope.type());
			return;
		}

		Message decoded;
		try {
			decoded = Protocol.decode(message.getPayload(), target);
		}
		catch (RuntimeException exception) {
			sendError(session, "invalid_message", "El mensaje no cumple el contrato del protocolo.");
			return;
		}

		dispatch(session, decoded);
	}

	private void dispatch(WebSocketSession session, Message message) {
		switch (message) {
			case Hello hello -> {
				session.getAttributes().put(NICKNAME, hello.nickname());
				sendWelcome(session);
			}
			case Echo echo -> send(session, new Echo(Protocol.VERSION, echo.id(), echo.at(), echo.text()));
			case Ping ping -> send(session, new Pong(Protocol.VERSION, ping.id(), System.currentTimeMillis()));
			default -> sendError(session, "unknown_type", "Mensaje no admitido del cliente.");
		}
	}

	private void sendWelcome(WebSocketSession session) {
		long now = System.currentTimeMillis();
		send(session, new Welcome(Protocol.VERSION, UUID.randomUUID().toString(), now,
				(String) session.getAttributes().get(CONNECTION_ID),
				(String) session.getAttributes().get(NICKNAME), now));
	}

	private void sendError(WebSocketSession session, String code, String message) {
		send(session, new ErrorMessage(Protocol.VERSION,
				UUID.randomUUID().toString(), System.currentTimeMillis(), code, message));
	}

	private void send(WebSocketSession session, Message message) {
		try {
			synchronized (session) {
				if (session.isOpen()) {
					session.sendMessage(new TextMessage(Protocol.encode(message)));
				}
			}
		}
		catch (IOException exception) {
			throw new UncheckedIOException("No se pudo enviar el mensaje", exception);
		}
	}
}
