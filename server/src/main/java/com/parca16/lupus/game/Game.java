package com.parca16.lupus.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import com.parca16.lupus.room.GameSettings;

/**
 * Partida en curso: jugadores, fases, temporizadores, reparto de roles y acciones de noche.
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
	private final Map<String, NightAction> nightActions = new LinkedHashMap<>();
	private final Map<String, Integer> livesAtNightStart = new LinkedHashMap<>();
	private final Map<String, Boolean> aliveAtNightStart = new LinkedHashMap<>();
	private final Set<String> readyToVote = new LinkedHashSet<>();
	private final Map<String, Vote> votes = new LinkedHashMap<>();
	private final List<String> runoffCandidates = new ArrayList<>();

	private Phase phase;
	private NightStep nightStep;
	private Instant deadline;
	private int round;
	private String presidentId;
	private Outcome outcome;
	private VoteResult lastVoteResult;
	private LynchResult lastLynch;
	private String wolfTargetId;
	private String seerTargetId;
	private NightResult lastNight;
	private DawnReport dawnReport;
	private int guardianProtections;
	private String lastGuardianTargetId;
	private int lastGuardianRound = -1;
	private boolean witchHealUsed;
	private boolean witchPoisonUsed;

	private Game(String roomCode, GameSettings settings, Clock clock, RandomGenerator random) {
		this.roomCode = roomCode;
		this.settings = settings;
		this.clock = clock;
		this.random = random;
	}

	public static Game start(String roomCode, GameSettings settings, List<GameParticipant> participants, Clock clock,
			RandomGenerator random) {
		Objects.requireNonNull(settings, "settings");
		Objects.requireNonNull(participants, "participants");
		settings.validatedForStart(participants.size());

		Game game = new Game(Objects.requireNonNull(roomCode, "roomCode"), settings,
				Objects.requireNonNull(clock, "clock"), Objects.requireNonNull(random, "random"));

		for (GameParticipant participant : participants) {
			GamePlayer player = new GamePlayer(participant.id(), participant.nickname(), settings.lives());
			if (game.players.putIfAbsent(participant.id(), player) != null) {
				throw new IllegalArgumentException("Jugador duplicado: " + participant.id());
			}
		}

		game.arrancar();
		return game;
	}

	/**
	 * Variante con roles prefijados, pensada para tests y escenarios.
	 *
	 * <p>El reparto normal usa {@link #start}.
	 */
	static Game startWithRoles(String roomCode, GameSettings settings, List<GameParticipant> participants,
			Map<String, Role> roles, Clock clock, RandomGenerator random) {
		Objects.requireNonNull(settings, "settings");
		Objects.requireNonNull(participants, "participants");
		Objects.requireNonNull(roles, "roles");
		settings.validatedForStart(participants.size());

		Game game = new Game(Objects.requireNonNull(roomCode, "roomCode"), settings,
				Objects.requireNonNull(clock, "clock"), Objects.requireNonNull(random, "random"));

		for (GameParticipant participant : participants) {
			Role role = roles.get(participant.id());
			if (role == null) {
				throw new IllegalArgumentException("Falta el rol de " + participant.id());
			}
			GamePlayer player = new GamePlayer(participant.id(), participant.nickname(), settings.lives());
			player.role(role);
			if (game.players.putIfAbsent(participant.id(), player) != null) {
				throw new IllegalArgumentException("Jugador duplicado: " + participant.id());
			}
		}

		game.arrancar();
		return game;
	}

	private void arrancar() {
		if (settings.president()) {
			begin(Phase.PRESIDENT_ELECTION, clock.instant());
		}
		else {
			begin(Phase.DEALING, clock.instant());
		}
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

	public NightResult lastNight() {
		return lastNight;
	}

	public DawnReport dawnReport() {
		return dawnReport;
	}

	public boolean finished() {
		return phase == Phase.FINISHED;
	}

	/** Elige al Presidente antes del reparto (regla D3). */
	public void electPresident(String playerId) {
		if (phase != Phase.PRESIDENT_ELECTION) {
			throw new IllegalStateException("El Presidente solo se elige al empezar la partida");
		}

		GamePlayer elected = requirePlayer(playerId);
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
				if (nightStep == NightStep.WOLVES) {
					resolveWolfVotes();
				}

				NightStep next = nextStep(nightStep);
				if (next == null) {
					resolveNight();
					checkVictory();
					if (!finished()) {
						begin(Phase.DAWN, now);
					}
				}
				else {
					nightStep = next;
					deadline = now.plus(durationOf(next));
				}
			}
			case DAWN -> begin(Phase.DEBATE, now);
			case DEBATE -> begin(Phase.VOTING, now);
			case VOTING, RUNOFF -> closeVoting(now);
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

	/**
	 * Aplica un golpe: lobos, veneno de la Bruja, Caballero o linchamiento.
	 *
	 * <p>Con vidas restantes se pierde una y se sobrevive; con cero vidas el golpe es muerte
	 * definitiva.
	 */
	public HitResult hit(String playerId) {
		GamePlayer player = requirePlayer(playerId);

		if (!player.alive()) {
			return HitResult.ALREADY_DEAD;
		}

		if (player.lives() > 0) {
			player.lives(player.lives() - 1);
			return HitResult.LOST_LIFE;
		}

		player.alive(false);
		return HitResult.DIED;
	}

	/** Restaura una vida sin superar el maximo configurado (pocion de la Bruja). */
	public boolean heal(String playerId) {
		GamePlayer player = requirePlayer(playerId);

		if (!player.alive() || player.lives() >= settings.lives()) {
			return false;
		}

		player.lives(player.lives() + 1);
		return true;
	}

	/** Muerte definitiva que ignora las vidas (muerte de amor de los enamorados). */
	public boolean kill(String playerId) {
		GamePlayer player = requirePlayer(playerId);

		if (!player.alive()) {
			return false;
		}

		player.lives(0);
		player.alive(false);
		return true;
	}

	public boolean isAlive(String playerId) {
		return requirePlayer(playerId).alive();
	}

	public int livesOf(String playerId) {
		return requirePlayer(playerId).lives();
	}

	/** Envia una accion de noche; valida fase, rol, ventana y objetivo. */
	public void submitNightAction(String playerId, NightAction action) {
		Objects.requireNonNull(action, "action");

		if (phase != Phase.NIGHT) {
			throw new GameRuleException(GameRuleException.NOT_YOUR_TURN, "No es la fase de noche.");
		}

		GamePlayer player = requirePlayer(playerId);
		if (!player.alive()) {
			throw new GameRuleException(GameRuleException.NOT_ALIVE, "Los muertos no actuan.");
		}

		switch (action) {
			case SeerAction seer -> actuarVidente(player, seer);
			case GuardianAction guardian -> actuarGuardian(player, guardian);
			case WitchAction witch -> actuarBruja(player, witch);
			case WolfVote vote -> votarLobo(player, vote);
		}
	}

	/** Rol que ve el Vidente esta noche; vacio para el resto. */
	public Optional<Role> seerVision(String playerId) {
		GamePlayer player = requirePlayer(playerId);
		if (player.role() != Role.SEER || seerTargetId == null) {
			return Optional.empty();
		}

		GamePlayer target = players.get(seerTargetId);
		return target == null ? Optional.empty() : Optional.ofNullable(target.role());
	}

	/** Victima elegida por los lobos; solo la ve la Bruja. */
	public Optional<String> wolfTargetFor(String playerId) {
		GamePlayer player = requirePlayer(playerId);
		if (player.role() != Role.WITCH) {
			return Optional.empty();
		}
		return Optional.ofNullable(wolfTargetId);
	}

	/**
	 * Marca al jugador como listo para votar (#41). Cuando todos los vivos lo han hecho, el debate
	 * termina y se pasa a la votacion.
	 *
	 * @return true si el debate ha terminado
	 */
	public boolean markReadyToVote(String playerId) {
		if (phase != Phase.DEBATE) {
			throw new GameRuleException(GameRuleException.NOT_YOUR_TURN, "Solo se puede marcar listo durante el debate.");
		}

		GamePlayer player = requirePlayer(playerId);
		if (!player.alive()) {
			throw new GameRuleException(GameRuleException.NOT_ALIVE, "Los muertos no pueden marcar listo.");
		}

		readyToVote.add(playerId);

		if (allReadyToVote()) {
			begin(Phase.VOTING, clock.instant());
			return true;
		}
		return false;
	}

	public boolean isReadyToVote(String playerId) {
		return readyToVote.contains(playerId);
	}

	public boolean allReadyToVote() {
		long alive = players.values().stream().filter(GamePlayer::alive).count();
		if (alive == 0) {
			return false;
		}
		long ready = readyToVote.stream()
				.map(players::get)
				.filter(player -> player != null && player.alive())
				.count();
		return ready >= alive;
	}

	/**
	 * Voto secreto durante la votacion o la segunda vuelta.
	 *
	 * @param targetId jugador votado; {@code null} es abstencion
	 */
	public void submitVote(String voterId, String targetId) {
		if (phase != Phase.VOTING && phase != Phase.RUNOFF) {
			throw new GameRuleException(GameRuleException.NOT_YOUR_TURN, "No hay votacion en curso.");
		}

		GamePlayer voter = requirePlayer(voterId);
		if (!voter.alive()) {
			throw new GameRuleException(GameRuleException.NOT_ALIVE, "Los muertos no votan.");
		}

		if (targetId != null) {
			GamePlayer target = players.get(targetId);
			boolean validTarget = target != null && target.alive() && !targetId.equals(voterId);
			if (!validTarget) {
				throw new GameRuleException(GameRuleException.INVALID_TARGET, "Objetivo de voto invalido.");
			}
			if (phase == Phase.RUNOFF && !runoffCandidates.contains(targetId)) {
				throw new GameRuleException(GameRuleException.INVALID_TARGET,
						"En la segunda vuelta solo se puede votar a los empatados.");
			}
		}

		votes.put(voterId, new Vote(voterId, targetId));

		if (allVotesSubmitted()) {
			closeVoting(clock.instant());
		}
	}

	public Optional<Vote> voteOf(String voterId) {
		requirePlayer(voterId);
		return Optional.ofNullable(votes.get(voterId));
	}

	public boolean allVotesSubmitted() {
		long alive = players.values().stream().filter(GamePlayer::alive).count();
		return alive > 0 && votes.size() >= alive;
	}

	/**
	 * Cierra la votacion: cuenta, decide si hay segunda vuelta y aplica el linchamiento.
	 *
	 * <p>La llamada elige sola cuando votan todos (o expira el plazo). Tambien se puede forzar para
	 * el cierre por tiempo.
	 */
	public VoteResult closeVoting(Instant now) {
		if (phase != Phase.VOTING && phase != Phase.RUNOFF) {
			throw new GameRuleException(GameRuleException.NOT_YOUR_TURN, "No hay votacion en curso.");
		}

		boolean runoff = phase == Phase.RUNOFF;
		VoteResult result = contarVotos(runoff);
		this.lastVoteResult = result;

		if (result.runoffNeeded()) {
			runoffCandidates.clear();
			runoffCandidates.addAll(result.tied());
			begin(Phase.RUNOFF, now);
			return result;
		}

		runoffCandidates.clear();
		applyLynch(result.lynchedId());
		checkVictory();

		if (!finished()) {
			round++;
			begin(Phase.NIGHT, now);
		}

		return result;
	}

	public VoteResult lastVoteResult() {
		return lastVoteResult;
	}

	public LynchResult lastLynch() {
		return lastLynch;
	}

	public List<String> runoffCandidates() {
		return List.copyOf(runoffCandidates);
	}

	/**
	 * Comprueba la victoria segun el reglamento: enamorados (cuando exista #50) -> lobos por
	 * paridad con voto doble del Presidente -> pueblo.
	 */
	public Outcome checkVictory() {
		if (finished()) {
			return outcome;
		}

		List<GamePlayer> vivos = players.values().stream().filter(GamePlayer::alive).toList();
		long lobos = vivos.stream().filter(player -> player.role() == Role.WOLF).count();
		long noLobos = vivos.size() - lobos;

		GamePlayer presidente = vivos.stream().filter(GamePlayer::president).findFirst().orElse(null);
		long votosLobos = lobos + (presidente != null && presidente.role() == Role.WOLF ? 1 : 0);
		long votosPueblo = noLobos + (presidente != null && presidente.role() != Role.WOLF ? 1 : 0);

		if (votosLobos >= votosPueblo) {
			finish(Outcome.WOLVES);
			return Outcome.WOLVES;
		}
		if (lobos == 0) {
			finish(Outcome.VILLAGE);
			return Outcome.VILLAGE;
		}

		return null;
	}

	private VoteResult contarVotos(boolean runoff) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		int abstenciones = 0;

		for (Vote vote : votes.values()) {
			if (vote.targetId() == null) {
				abstenciones++;
				continue;
			}
			int peso = isPresident(vote.voterId()) ? 2 : 1;
			counts.merge(vote.targetId(), peso, Integer::sum);
		}

		List<String> tied = new ArrayList<>();
		String lynched = null;
		boolean runoffNeeded = false;

		if (!counts.isEmpty()) {
			int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
			tied = counts.entrySet().stream()
					.filter(entry -> entry.getValue() == max)
					.map(Map.Entry::getKey)
					.toList();

			if (tied.size() == 1) {
				lynched = tied.get(0);
			}
			else if (!runoff) {
				runoffNeeded = true;
			}
		}

		List<Vote> details = settings.showVoteDetail() ? List.copyOf(votes.values()) : List.of();
		return new VoteResult(lynched, runoffNeeded, tied, counts, details, abstenciones);
	}

	private void applyLynch(String targetId) {
		if (targetId == null) {
			lastLynch = new LynchResult(null, false, false, null, null);
			return;
		}

		HitResult result = hit(targetId);
		GamePlayer target = requirePlayer(targetId);
		boolean died = result == HitResult.DIED;

		lastLynch = new LynchResult(targetId, result == HitResult.LOST_LIFE, died,
				died ? target.role() : null, result);
	}

	private boolean isPresident(String playerId) {
		GamePlayer player = players.get(playerId);
		return player != null && player.alive() && player.president();
	}

	/** True si el actor (o todos los lobos) ya han actuado en el paso actual. */
	public boolean currentStepDone() {		if (phase != Phase.NIGHT || nightStep == null) {
			return false;
		}

		return switch (nightStep) {
			case SEER -> hasAction(SeerAction.class);
			case GUARDIAN -> hasAction(GuardianAction.class);
			case WITCH -> hasAction(WitchAction.class);
			case WOLVES -> allWolvesVoted();
			case CUPID -> false;
		};
	}

	/** Vista de la partida para un jugador; solo incluye su propio rol. */
	public GameView viewFor(String playerId) {
		GamePlayer player = requirePlayer(playerId);

		List<GameView.PublicPlayer> publicPlayers = players.values().stream()
				.map(other -> new GameView.PublicPlayer(other.id(), other.nickname(), other.lives(),
						other.alive(), other.president()))
				.toList();

		return new GameView(roomCode, phase, round, nightStep, deadline, player.id(), player.role(),
				player.lives(), player.alive(), player.president(), publicPlayers);
	}

	private void actuarVidente(GamePlayer player, SeerAction action) {
		requierePaso(NightStep.SEER);
		requiereRol(player, Role.SEER);
		requiereSinActuar(player);
		requiereObjetivoVivo(action.targetId());

		nightActions.put(player.id(), action);
		seerTargetId = action.targetId();
	}

	private void actuarGuardian(GamePlayer player, GuardianAction action) {
		requierePaso(NightStep.GUARDIAN);
		requiereRol(player, Role.GUARDIAN);
		requiereSinActuar(player);
		requiereObjetivoVivo(action.targetId());

		if (guardianProtections >= 3) {
			throw new GameRuleException(GameRuleException.GUARDIAN_LIMIT,
					"El Angel de la Guarda ya ha usado sus 3 protecciones.");
		}
		if (lastGuardianRound == round - 1 && action.targetId().equals(lastGuardianTargetId)) {
			throw new GameRuleException(GameRuleException.REPEATED_TARGET,
					"No puedes proteger al mismo jugador dos noches seguidas.");
		}

		guardianProtections++;
		lastGuardianTargetId = action.targetId();
		lastGuardianRound = round;
		nightActions.put(player.id(), action);
	}

	private void actuarBruja(GamePlayer player, WitchAction action) {
		requierePaso(NightStep.WITCH);
		requiereRol(player, Role.WITCH);
		requiereSinActuar(player);

		switch (action.potion()) {
			case NONE -> {
			}
			case HEAL -> {
				if (witchHealUsed) {
					throw new GameRuleException(GameRuleException.NO_POTIONS,
							"La pocion de vida ya se ha usado.");
				}
				requiereObjetivoVivo(action.targetId());
				witchHealUsed = true;
			}
			case POISON -> {
				if (witchPoisonUsed) {
					throw new GameRuleException(GameRuleException.NO_POTIONS,
							"La pocion de muerte ya se ha usado.");
				}
				requiereObjetivoVivo(action.targetId());
				witchPoisonUsed = true;
			}
		}

		nightActions.put(player.id(), action);
	}

	private void votarLobo(GamePlayer player, WolfVote vote) {
		requierePaso(NightStep.WOLVES);
		requiereRol(player, Role.WOLF);
		requiereObjetivoVivo(vote.targetId());

		nightActions.put(player.id(), vote);
	}

	private void requierePaso(NightStep expected) {
		if (nightStep != expected) {
			throw new GameRuleException(GameRuleException.NOT_YOUR_TURN, "No es tu turno de actuar.");
		}
	}

	private static void requiereRol(GamePlayer player, Role expected) {
		if (player.role() != expected) {
			throw new GameRuleException(GameRuleException.INVALID_ACTION,
					"Tu rol no puede hacer esa accion.");
		}
	}

	private void requiereSinActuar(GamePlayer player) {
		if (nightActions.containsKey(player.id())) {
			throw new GameRuleException(GameRuleException.ALREADY_ACTED, "Ya has actuado esta noche.");
		}
	}

	private void requiereObjetivoVivo(String targetId) {
		GamePlayer target = targetId == null ? null : players.get(targetId);
		if (target == null || !target.alive()) {
			throw new GameRuleException(GameRuleException.INVALID_TARGET, "Objetivo invalido.");
		}
	}

	private boolean hasAction(Class<? extends NightAction> type) {
		return nightActions.values().stream().anyMatch(type::isInstance);
	}

	private boolean allWolvesVoted() {
		long aliveWolves = players.values().stream()
				.filter(player -> player.alive() && player.role() == Role.WOLF)
				.count();
		if (aliveWolves == 0) {
			return true;
		}
		long votes = nightActions.values().stream().filter(WolfVote.class::isInstance).count();
		return votes >= aliveWolves;
	}

	private void resolveWolfVotes() {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (NightAction action : nightActions.values()) {
			if (action instanceof WolfVote vote) {
				counts.merge(vote.targetId(), 1, Integer::sum);
			}
		}

		if (counts.isEmpty()) {
			wolfTargetId = null;
			return;
		}

		int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		List<String> tied = counts.entrySet().stream()
				.filter(entry -> entry.getValue() == max)
				.map(Map.Entry::getKey)
				.toList();

		wolfTargetId = tied.get(random.nextInt(tied.size()));
	}

	/** Resolucion de la noche: proteccion, ataque, pocion y muertes (#39). */
	private void resolveNight() {
		List<NightEvent> events = new ArrayList<>();
		String protectedTarget = guardianTarget();
		boolean wolfBlocked = wolfTargetId != null && wolfTargetId.equals(protectedTarget);

		if (wolfTargetId != null) {
			if (wolfBlocked) {
				events.add(new NightEvent(NightEvent.Type.WOLF_ATTACK, wolfTargetId, true, false, null));
			}
			else {
				HitResult result = hit(wolfTargetId);
				events.add(new NightEvent(NightEvent.Type.WOLF_ATTACK, wolfTargetId, false, true, result));
			}
		}

		WitchAction witch = witchAction();
		GamePlayer bruja = witch == null ? null : players.get(witchActorId());
		if (witch != null && bruja != null && bruja.alive()) {
			switch (witch.potion()) {
				case HEAL -> {
					boolean applied = heal(witch.targetId());
					events.add(new NightEvent(NightEvent.Type.HEAL, witch.targetId(), false, applied, null));
				}
				case POISON -> {
					boolean blocked = witch.targetId() != null && witch.targetId().equals(protectedTarget);
					if (blocked) {
						events.add(new NightEvent(NightEvent.Type.POISON, witch.targetId(), true, false, null));
					}
					else {
						HitResult result = hit(witch.targetId());
						events.add(new NightEvent(NightEvent.Type.POISON, witch.targetId(), false, true, result));
					}
				}
				case NONE -> {
				}
			}
		}

		lastNight = new NightResult(wolfTargetId, wolfBlocked, events);
	}

	private String guardianTarget() {		return nightActions.values().stream()
				.filter(GuardianAction.class::isInstance)
				.map(NightAction::targetId)
				.findFirst()
				.orElse(null);
	}

	private WitchAction witchAction() {
		return nightActions.values().stream()
				.filter(WitchAction.class::isInstance)
				.map(WitchAction.class::cast)
				.findFirst()
				.orElse(null);
	}

	private String witchActorId() {
		return nightActions.entrySet().stream()
				.filter(entry -> entry.getValue() instanceof WitchAction)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	private GamePlayer requirePlayer(String playerId) {
		GamePlayer player = players.get(playerId);
		if (player == null) {
			throw new IllegalArgumentException("Jugador desconocido: " + playerId);
		}
		return player;
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
				resetNight();
				NightStep first = firstStep();
				if (first == null) {
					begin(Phase.DAWN, now);
					return;
				}
				this.nightStep = first;
				this.deadline = now.plus(durationOf(first));
			}
			case DAWN -> {
				this.dawnReport = buildDawnReport();
				this.deadline = now.plus(DAWN_TIME);
			}
			case DEBATE -> {
				readyToVote.clear();
				this.deadline = now.plusSeconds(settings.debateSeconds());
			}
			case VOTING -> {
				votes.clear();
				runoffCandidates.clear();
				lastVoteResult = null;
				lastLynch = null;
				this.deadline = now.plusSeconds(settings.voteSeconds());
			}
			case RUNOFF -> {
				votes.clear();
				this.deadline = now.plus(RUNOFF_TIME);
			}
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

	private void resetNight() {
		nightActions.clear();
		wolfTargetId = null;
		seerTargetId = null;
		lastNight = null;
		dawnReport = null;

		livesAtNightStart.clear();
		aliveAtNightStart.clear();
		for (GamePlayer player : players.values()) {
			livesAtNightStart.put(player.id(), player.lives());
			aliveAtNightStart.put(player.id(), player.alive());
		}
	}

	/** Parte del amanecer comparando el estado actual con el inicio de la noche (#40). */
	private DawnReport buildDawnReport() {
		List<DawnEvent> events = new ArrayList<>();

		for (GamePlayer player : players.values()) {
			Boolean wasAlive = aliveAtNightStart.get(player.id());
			Integer livesBefore = livesAtNightStart.get(player.id());

			if (wasAlive == null || livesBefore == null || !wasAlive) {
				continue;
			}

			if (!player.alive()) {
				events.add(new DawnEvent(DawnEvent.Type.DIED, player.id(), player.nickname(), 0));
			}
			else if (player.lives() < livesBefore) {
				events.add(new DawnEvent(DawnEvent.Type.LOST_LIFE, player.id(), player.nickname(), player.lives()));
			}
			else if (player.lives() > livesBefore) {
				events.add(new DawnEvent(DawnEvent.Type.HEALED, player.id(), player.nickname(), player.lives()));
			}
		}

		return new DawnReport(events);
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
