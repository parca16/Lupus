package com.parca16.lupus.ws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** Limite de mensajes por conexion (#74). */
class MessageRateLimiterTest {

	@Test
	void permiteHastaElLimiteYDesspuesRechaza() {
		AtomicLong now = new AtomicLong(1000);
		MessageRateLimiter limiter = new MessageRateLimiter(3, 1000, now::get);

		assertTrue(limiter.allow("s-1"));
		assertTrue(limiter.allow("s-1"));
		assertTrue(limiter.allow("s-1"));
		assertFalse(limiter.allow("s-1"));
	}

	@Test
	void laVentanaSeRenueva() {
		AtomicLong now = new AtomicLong(1000);
		MessageRateLimiter limiter = new MessageRateLimiter(1, 1000, now::get);

		assertTrue(limiter.allow("s-1"));
		assertFalse(limiter.allow("s-1"));

		now.addAndGet(1000);
		assertTrue(limiter.allow("s-1"));
	}

	@Test
	void cadaConexionTieneSuPropioContador() {
		AtomicLong now = new AtomicLong(1000);
		MessageRateLimiter limiter = new MessageRateLimiter(1, 1000, now::get);

		assertTrue(limiter.allow("s-1"));
		assertTrue(limiter.allow("s-2"));
		assertFalse(limiter.allow("s-1"));
	}
}
