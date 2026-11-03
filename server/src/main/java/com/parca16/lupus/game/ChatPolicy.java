package com.parca16.lupus.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Visibilidad del chat por fase, rol y estado (#63). */
public final class ChatPolicy {

	private static final Set<Phase> PUBLIC_PHASES = Set.of(Phase.DEBATE, Phase.VOTING, Phase.RUNOFF);

	private ChatPolicy() {
	}

	/** True si el jugador puede escribir en el canal en este momento. */
	public static boolean canPost(Game game, String playerId, ChatChannel channel) {
		GamePlayer player = find(game, playerId);
		if (player == null || !player.alive() || game.finished()) {
			return false;
		}

		return switch (channel) {
			case PUBLIC -> PUBLIC_PHASES.contains(game.phase());
			case WOLVES -> game.phase() == Phase.NIGHT && player.role() == Role.WOLF;
			case LOVERS -> lovesAlivePlayer(game, player);
		};
	}

	/** Jugadores que reciben los mensajes de un canal. */
	public static List<GamePlayer> recipients(Game game, ChatChannel channel) {
		return switch (channel) {
			case PUBLIC -> game.players();
			case WOLVES -> game.players().stream()
					.filter(player -> player.alive() && player.role() == Role.WOLF)
					.toList();
			case LOVERS -> {
				Set<String> ids = new HashSet<>();
				for (GamePlayer player : game.players()) {
					if (player.loverId() != null) {
						ids.add(player.id());
						ids.add(player.loverId());
					}
				}
				yield game.players().stream()
						.filter(player -> ids.contains(player.id()))
						.toList();
			}
		};
	}

	/** Canales disponibles para un jugador, de más público a más privado. */
	public static List<ChatChannel> available(Game game, String playerId) {
		return List.of(ChatChannel.PUBLIC, ChatChannel.WOLVES, ChatChannel.LOVERS).stream()
				.filter(channel -> canPost(game, playerId, channel))
				.toList();
	}

	private static boolean lovesAlivePlayer(Game game, GamePlayer player) {
		if (player.loverId() == null) {
			return false;
		}
		return game.players().stream()
				.anyMatch(other -> other.id().equals(player.loverId()) && other.alive());
	}

	private static GamePlayer find(Game game, String playerId) {
		return game.players().stream()
				.filter(player -> player.id().equals(playerId))
				.findFirst()
				.orElse(null);
	}
}
