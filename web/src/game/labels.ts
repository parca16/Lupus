/** Etiquetas en castellano para las fases y roles del motor. */

export const PHASE_LABELS: Record<string, string> = {
  PRESIDENT_ELECTION: 'Elección de Presidente',
  DEALING: 'Reparto',
  REVEAL: 'Revelación',
  NIGHT: 'Noche',
  DAWN: 'Amanecer',
  KNIGHT_REACTION: 'El Caballero golpea',
  DEBATE: 'Debate',
  VOTING: 'Votación',
  RUNOFF: 'Segunda vuelta',
  FINISHED: 'Fin de la partida',
}

export const PHASE_DESCRIPTIONS: Record<string, string> = {
  PRESIDENT_ELECTION: 'Votad quién será el Presidente. Su voto valdrá doble.',
  DEALING: 'Repartiendo las cartas…',
  REVEAL: 'Mira tu carta en secreto. Solo tú puedes verla.',
  NIGHT: 'El pueblo duerme. Los roles actúan en su ventana.',
  DAWN: 'Amanece y se revelan los sucesos de la noche.',
  KNIGHT_REACTION: 'El Caballero ha muerto y elige a quién golpear.',
  DEBATE: 'Debatid quién es un lobo antes de votar.',
  VOTING: 'Votación secreta para linchar a un sospechoso.',
  RUNOFF: 'Segunda vuelta entre los más votados.',
  FINISHED: 'La partida ha terminado.',
}

export const NIGHT_STEP_LABELS: Record<string, string> = {
  CUPID: 'Cupido',
  SEER: 'Vidente',
  GUARDIAN: 'Ángel de la Guarda',
  WOLVES: 'Lobos',
  WITCH: 'Bruja',
}

/** Rol del motor (mayúsculas) al rol que actúa en cada paso. */
export const STEP_ROLE: Record<string, string> = {
  CUPID: 'CUPID',
  SEER: 'SEER',
  GUARDIAN: 'GUARDIAN',
  WOLVES: 'WOLF',
  WITCH: 'WITCH',
}

export const GAME_ROLE_LABELS: Record<string, string> = {
  WOLF: 'Lobo',
  VILLAGER: 'Aldeano',
  SEER: 'Vidente',
  WITCH: 'Bruja',
  KNIGHT: 'Caballero de la Espada Oxidada',
  GUARDIAN: 'Ángel de la Guarda',
  CUPID: 'Cupido',
}

export const GAME_ROLE_DESCRIPTIONS: Record<string, string> = {
  WOLF: 'Cada noche eliges con tus compañeros a quién atacar. Gana cuando los lobos igualan o superan a los demás.',
  VILLAGER: 'No tienes poderes. Tu arma es el debate y el voto.',
  SEER: 'Cada noche descubres el rol de un jugador.',
  WITCH: 'Tienes una poción de vida y otra de muerte, de un solo uso cada una.',
  KNIGHT: 'Si mueres, eliges a alguien para golpearle.',
  GUARDIAN: 'Cada noche proteges a un jugador de los lobos y del veneno. Tienes 3 protecciones.',
  CUPID: 'La primera noche eliges a los dos enamorados.',
}
