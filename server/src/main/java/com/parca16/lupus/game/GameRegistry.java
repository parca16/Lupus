package com.parca16.lupus.game;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Partidas en curso, una por sala. */
@Component
public class GameRegistry {

	private final Map<String, Game> games = new ConcurrentHashMap<>();

	public void put(Game game) {
		games.put(game.roomCode(), game);
	}

	public Optional<Game> find(String roomCode) {
		return roomCode == null ? Optional.empty() : Optional.ofNullable(games.get(roomCode));
	}

	public void remove(String roomCode) {
		if (roomCode != null) {
			games.remove(roomCode);
		}
	}

	public List<Game> all() {
		return List.copyOf(games.values());
	}
}
