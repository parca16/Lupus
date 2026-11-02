package com.parca16.lupus.lifecycle;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parca16.lupus.room.Room;
import com.parca16.lupus.room.RoomRegistry;
import com.parca16.lupus.ws.RoomNotifier;

/**
 * Destruye las salas sin actividad (issue #25).
 *
 * <p>Cualquier mensaje de un jugador de la sala reinicia el contador. Al caducar, los jugadores
 * conectados reciben {@code room_closed} con motivo {@code inactivity}.
 */
@Component
public class RoomJanitor {

	private final RoomRegistry rooms;
	private final RoomNotifier notifier;
	private final Clock clock;
	private final Duration ttl;

	public RoomJanitor(RoomRegistry rooms, RoomNotifier notifier, Clock clock,
			@Value("${lupus.rooms.expiration-millis:1800000}") long expirationMillis) {
		this.rooms = rooms;
		this.notifier = notifier;
		this.clock = clock;
		this.ttl = Duration.ofMillis(expirationMillis);
	}

	@Scheduled(fixedDelayString = "${lupus.rooms.sweep-millis:60000}")
	public void barrer() {
		for (Room room : rooms.removeExpired(clock.instant(), ttl)) {
			notifier.close(room, RoomNotifier.INACTIVITY, null);
		}
	}
}
