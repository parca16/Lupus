package com.parca16.lupus.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Limita los mensajes por conexion para evitar floods (#74). */
@Component
public class MessageRateLimiter {

	private final int maxMessages;
	private final long windowMillis;
	private final LongSupplier clock;
	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	@Autowired
	public MessageRateLimiter(@Value("${lupus.rate.max-messages:60}") int maxMessages,
			@Value("${lupus.rate.window-millis:10000}") long windowMillis) {
		this(maxMessages, windowMillis, System::currentTimeMillis);
	}

	MessageRateLimiter(int maxMessages, long windowMillis, LongSupplier clock) {
		this.maxMessages = maxMessages;
		this.windowMillis = windowMillis;
		this.clock = clock;
	}

	public boolean allow(String sessionId) {
		long now = clock.getAsLong();
		Window window = windows.computeIfAbsent(sessionId, id -> new Window(now));
		return window.allow(now, maxMessages, windowMillis);
	}

	public void forget(String sessionId) {
		windows.remove(sessionId);
	}

	private static final class Window {

		private long start;
		private int count;

		Window(long start) {
			this.start = start;
		}

		synchronized boolean allow(long now, int maxMessages, long windowMillis) {
			if (now - start >= windowMillis) {
				start = now;
				count = 0;
			}
			count++;
			return count <= maxMessages;
		}
	}
}
