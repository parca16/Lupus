package com.parca16.lupus.game;

/** Error de reglas al intentar una accion. El codigo viaja al cliente. */
public class GameRuleException extends RuntimeException {

	public static final String NOT_YOUR_TURN = "not_your_turn";
	public static final String NOT_ALIVE = "not_alive";
	public static final String INVALID_TARGET = "invalid_target";
	public static final String ALREADY_ACTED = "already_acted";
	public static final String GUARDIAN_LIMIT = "guardian_limit";
	public static final String REPEATED_TARGET = "repeated_target";
	public static final String NO_POTIONS = "no_potions_left";
	public static final String INVALID_ACTION = "invalid_action";

	private final String code;

	public GameRuleException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
