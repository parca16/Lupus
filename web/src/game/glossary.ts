/** Contenido del glosario de roles (#77), basado en docs/reglamento.md. */

export interface GlossaryEntry {
  id: string
  label: string
  team: string
  summary: string
  details: string
}

export const ROLE_GLOSSARY: GlossaryEntry[] = [
  {
    id: 'WOLF',
    label: 'Lobo',
    team: 'Lobos',
    summary: 'Cada noche la manada elige a quién atacar.',
    details:
      'Gana cuando los lobos igualan o superan en número a los demás vivos. Durante la noche votan en secreto y deciden una única víctima.',
  },
  {
    id: 'VILLAGER',
    label: 'Aldeano',
    team: 'Pueblo',
    summary: 'No tiene poderes: su arma es el debate y el voto.',
    details:
      'Gana cuando muere el último lobo. De día participa en el debate y en la votación para linchar a un sospechoso.',
  },
  {
    id: 'SEER',
    label: 'Vidente',
    team: 'Pueblo',
    summary: 'Cada noche descubre el rol de un jugador.',
    details:
      'Ve el rol base del objetivo, pero no su cargo de Presidente ni si está enamorado. Es una de las cartas más valiosas del pueblo.',
  },
  {
    id: 'WITCH',
    label: 'Bruja',
    team: 'Pueblo',
    summary: 'Tiene una poción de vida y otra de muerte, de un solo uso.',
    details:
      'Puede usar como máximo una poción por noche. La de vida restaura una vida; la de muerte quita una. Si los lobos la matan esa misma noche, no llega a actuar.',
  },
  {
    id: 'KNIGHT',
    label: 'Caballero de la Espada Oxidada',
    team: 'Pueblo',
    summary: 'Si muere, elige a alguien para golpearle.',
    details:
      'El golpe quita una vida y se resuelve antes de la siguiente votación. Si muere linchado, elige inmediatamente después del linchamiento.',
  },
  {
    id: 'GUARDIAN',
    label: 'Ángel de la Guarda',
    team: 'Pueblo',
    summary: 'Protege a un jugador cada noche; tiene 3 protecciones.',
    details:
      'Puede protegerse a sí mismo, pero no al mismo jugador dos noches seguidas. La protección anula el ataque de los lobos y la poción de muerte, pero no el golpe del Caballero ni el linchamiento.',
  },
  {
    id: 'CUPID',
    label: 'Cupido',
    team: 'Pueblo',
    summary: 'La primera noche elige a los dos enamorados.',
    details:
      'Puede elegirse a sí mismo. Los enamorados se reconocen y, si son de bandos distintos, forman un tercer bando que gana si son los últimos vivos. Si uno muere, el otro muere con él.',
  },
  {
    id: 'PRESIDENT',
    label: 'Presidente',
    team: 'Cargo',
    summary: 'Su voto vale doble en el linchamiento.',
    details:
      'Se elige al empezar la partida si el host lo activa. El cargo es público y se pierde al morir. No es un rol: se suma a la carta que tengas.',
  },
]
