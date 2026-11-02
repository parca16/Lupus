package com.parca16.lupus.game;

/** Estado de un jugador dentro de la partida. */
public final class GamePlayer {

	private final String id;
	private final String nickname;
	private Role role;
	private int lives;
	private boolean alive = true;
	private boolean president;

	GamePlayer(String id, String nickname, int lives) {
		this.id = id;
		this.nickname = nickname;
		this.lives = lives;
	}

	public String id() {
		return id;
	}

	public String nickname() {
		return nickname;
	}

	public Role role() {
		return role;
	}

	void role(Role role) {
		this.role = role;
	}

	public int lives() {
		return lives;
	}

	void lives(int lives) {
		this.lives = lives;
	}

	public boolean alive() {
		return alive;
	}

	void alive(boolean alive) {
		this.alive = alive;
	}

	public boolean president() {
		return president;
	}

	void president(boolean president) {
		this.president = president;
	}
}
