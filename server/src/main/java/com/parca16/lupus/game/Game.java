package com.parca16.lupus.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import com.parca16.lupus.room.GameSettings;

/**
 * Partida en curso: jugadores, fases, temporizadores y reparto de roles.
 *
 * <p>Es dominio puro: no conoce WebSocket ni Spring. El servidor es la unica autoridad y cada
 * jugador recibe su propia {@link GameView}, que nunca incluye los roles ajenos.
 */
public final class Game {

	public static final Duration PRESIDENT_ELECTION_TIME = Duration.ofSeconds(45);
	public static final Duration REVEAL_TIME = Duration.ofSeconds(10);
	public static final Duration DAWN_TIME = Duration.ofSeconds(10);
	public static final Duration RUNOFF_TIME = Duration.ofSeconds(30);
	public static final Duration MIN_WOLF_WINDOW = Duration.ofSeconds(40);

	private static final List<NightStep> NIGHT_ORDER = List.of(NightStep.CUPID, NightStep.SEER,
			NightStep.GUARDIAN, NightStep.WOLVES, NightStep.WITCH);

	private final String roomCode;
	private final GameSettings settings;
	private final Clock clock;
	private final RandomGenerator random;
	private final Map<String, GamePlayer> players = new LinkedHashMap<>();

	private Phase phase;
	private NightStep nightStep;
	private Instant deadline;
	private int round;
	private String presidentId;
	private Outcome outcome;

	private Game(String roomCode, GameSettings settings, Clock clock, RandomGenerator random) {
		this.roomCode = roomCode;
		this.settings = settings;
		this.clock = clock;
		this.random = random;
	}

	public static Game start(String roomCode, GameSettings settings, List<GameParticipant> participants, Clock clock,
			RandomGenerator random) {
		Objects.requireNonNull(roomCode, "roomCode");
		Objects.requireNonNull(settings, "settings");
		Objects.requireNonNull(participants, "participants");
		Objects.requireNonNull(clock, "clock");
		Objects.requireNonNull(random, "random");

		settings.validatedForStart(participants.size());

		Game game = new Game(roomCode, settings, clock, random);
		for (GameParticipant participant : participants) {
			if (game.players.putIfAbsent(participant.id(),
					new GamePlayer(participant.id(), participant.nickname(), settings.lives())) != null) {
				throw new IllegalArgumentException("Jugador duplicado: " + participant.id());
			}
		}

		if (settings.president()) {
			game.begin(Phase.PRESIDENT_ELECTION, clock.instant());
		}
		else {
			game.begin(Phase.DEALING, clock.instant());
		}
		return game;
	}

	public String roomCode() {
		return roomCode;
	}

	public GameSettings settings() {
		return settings;
	}

	public Phase phase() {
		return phase;
	}

	public NightStep nightStep() {
		return nightStep;
	}

	public Instant deadline() {
		return deadline;
	}

	public int round() {
		return round;
	}

	public String presidentId() {
		return presidentId;
	}

	public Outcome outcome() {
		return outcome;
	}

	public boolean finished() {
		return phase == Phase.FINISHED;
	}

	/** Elige al Presidente antes del reparto (regla D3). */
	public void electPresident(String playerId) {
		if (phase != Phase.PRESIDENT_ELECTION) {
			throw new IllegalStateException("El Presidente solo se elige al empezar la partida");
		}

		GamePlayer elected = players.get(playerId);
		if (elected == null) {
			throw new IllegalArgumentException("Jugador desconocido: " + playerId);
		}

		players.values().forEach(player -> player.president(false));
		elected.president(true);
		this.presidentId = playerId;

		begin(Phase.DEALING, clock.instant());
	}

	/** Pasa de la votacion a la segunda vuelta (issue #42). */
	public void startRunoff() {
		if (phase != Phase.VOTING) {
			throw new IllegalStateException("La segunda vuelta solo empieza desde la votacion");
		}
		begin(Phase.RUNOFF, clock.instant());
	}

	/** Avanza una fase o un paso de la noche. */
	public void advance(Instant now) {
		Objects.requireNonNull(now, "now");

		switch (phase) {
			case PRESIDENT_ELECTION -> {
				if (presidentId == null) {
					throw new IllegalStateException("Falta elegir al Presidente antes del reparto");
				}
				begin(Phase.DEALING, now);
			}
			case DEALING -> begin(Phase.REVEAL, now);
			case REVEAL -> {
				round = 1;
				begin(Phase.NIGHT, now);
			}
			case NIGHT -> {
				NightStep next = nextStep(nightStep);
				if (next == null) {
					begin(Phase.DAWN, now);
				}
				else {
					nightStep = next;
					deadline = now.plus(durationOf(next));
				}
			}
			case DAWN -> begin(Phase.DEBATE, now);
			case DEBATE -> begin(Phase.VOTING, now);
			case VOTING -> {
				round++;
				begin(Phase.NIGHT, now);
			}
			case RUNOFF -> {
				round++;
				begin(Phase.NIGHT, now);
			}
			case FINISHED -> throw new IllegalStateException("La partida ya ha terminado");
		}
	}

	/**
	 * Avanza sola mientras los plazos hayan vencido. Se detiene en la eleccion de Presidente si
	 * todavia no hay resultado, porque necesita una accion externa.
	 */
	public Phase tick(Instant now) {
		Objects.requireNonNull(now, "now");

		int guard = 0;
		while (phase != Phase.FINISHED && deadline != null && !deadline.isAfter(now) && guard++ < 20) {
			if (phase == Phase.PRESIDENT_ELECTION && presidentId == null) {
				break;
			}
			advance(now);
		}
		return phase;
	}

	public void finish(Outcome outcome) {
		this.outcome = Objects.requireNonNull(outcome, "outcome");
		this.phase = Phase.FINISHED;
		this.nightStep = null;
		this.deadline = null;
	}

	/** Vista de la partida para un jugador; solo incluye su propio rol. */
	public GameView viewFor(String playerId) {
		GamePlayer player = players.get(playerId);
		if (player == null) {
			throw new IllegalArgumentException("Jugador desconocido: " + playerId);
		}

		List<GameView.PublicPlayer> publicPlayers = players.values().stream()
				.map(other -> new GameView.PublicPlayer(other.id(), other.nickname(), other.lives(),
						other.alive(), other.president()))
				.toList();

		return new GameView(roomCode, phase, round, nightStep, deadline, player.id(), player.role(),
				player.lives(), player.alive(), player.president(), publicPlayers);
	}

	private void begin(Phase next, Instant now) {
		this.phase = next;
		this.nightStep = null;

		switch (next) {
			case DEALING -> {
				repartir();
				this.deadline = now;
			}
			case PRESIDENT_ELECTION -> this.deadline = now.plus(PRESIDENT_ELECTION_TIME);
			case REVEAL -> this.deadline = now.plus(REVEAL_TIME);
			case NIGHT -> {
				NightStep first = firstStep();
				if (first == null) {
					begin(Phase.DAWN, now);
					return;
				}
				this.nightStep = first;
				this.deadline = now.plus(durationOf(first));
			}
			case DAWN -> this.deadline = now.plus(DAWN_TIME);
			case DEBATE -> this.deadline = now.plusSeconds(settings.debateSeconds());
			case VOTING -> this.deadline = now.plusSeconds(settings.voteSeconds());
			case RUNOFF -> this.deadline = now.plus(RUNOFF_TIME);
			case FINISHED -> this.deadline = null;
		}
	}

	private void repartir() {
		boolean alreadyDealt = players.values().stream().allMatch(player -> player.role() != null);
		if (alreadyDealt) {
			return;
		}

		RoleDealer.deal(settings, List.copyOf(players.keySet()), random)
				.forEach((playerId, role) -> players.get(playerId).role(role));
	}

	private NightStep firstStep() {
		for (NightStep step : NIGHT_ORDER) {
			if (available(step)) {
				return step;
			}
		}
		return null;
	}

	private NightStep nextStep(NightStep current) {
		int index = NIGHT_ORDER.indexOf(current);
		for (int i = index + 1; i < NIGHT_ORDER.size(); i++) {
			if (available(NIGHT_ORDER.get(i))) {
				return NIGHT_ORDER.get(i);
			}
		}
		return null;
	}

	private boolean available(NightStep step) {
		if (step == NightStep.CUPID && round != 1) {
			return false;
		}
		return players.values().stream().anyMatch(player -> player.alive() && player.role() == roleOf(step));
	}

	private Duration durationOf(NightStep step) {
		Duration roleTime = Duration.ofSeconds(settings.roleSeconds());
		if (step == NightStep.WOLVES && roleTime.compareTo(MIN_WOLF_WINDOW) < 0) {
			return MIN_WOLF_WINDOW;
		}
		return roleTime;
	}

	private static Role roleOf(NightStep step) {
		return switch (step) {
			case CUPID -> Role.CUPID;
			case SEER -> Role.SEER;
			case GUARDIAN -> Role.GUARDIAN;
			case WOLVES -> Role.WOLF;
			case WITCH -> Role.WITCH;
		};
	}
}
