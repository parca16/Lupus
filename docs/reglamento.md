# Reglamento de Lupus

> **Estado:** propuesta v0.1 para validar (issue #75).
> Las reglas marcadas como **✅ recomendado** se implementarán tal cual salvo que se indique lo
> contrario en la revisión. Las dudas están agrupadas al final, en [Decisiones abiertas](#decisiones-abiertas).
>
> Este documento es la referencia del motor (#34–#45) y del glosario (#77).

## 1. Resumen

Lupus es un juego de deducción social para **5 a 16 jugadores** en el que dos bandos se enfrentan:
el **pueblo** y los **lobos**. El servidor reparte los roles, dirige las fases y decide todo; los
jugadores solo ven su carta y actúan en su turno.

- **Gana el pueblo** si elimina a todos los lobos.
- **Ganan los lobos** si eliminan a todos los no lobos.
- Los **enamorados de bandos distintos** forman un tercer bando y ganan solos si son los últimos
  dos vivos.

Una partida dura entre **10 y 15 minutos** con los tiempos por defecto.

## 2. Jugadores y composición

- Mínimo **5** jugadores conectados para empezar; máximo **16** por sala.
- **Hay una carta por jugador.** Las cartas se reparten así:
  `aldeanos = jugadores − lobos − roles especiales`.
- Siempre debe quedar **al menos un aldeano** (validación de #27).
- Todos los roles especiales son **únicos**; no se puede repetir ninguno.
- La composición la decide el host en el lobby y se puede cambiar hasta que empieza la partida.

### Composición recomendada

Es una recomendación de equilibrio, no una restricción del servidor:

| Jugadores | Lobos | Roles especiales sugeridos | Aldeanos | Presidente |
| --------- | ----- | -------------------------- | -------- | ---------- |
| 5–7       | 1     | Vidente                    | 3–5      | opcional   |
| 8–10      | 2     | Vidente + Bruja            | 4–6      | opcional   |
| 11–13     | 2–3   | Vidente + Bruja + Ángel    | 5–7      | opcional   |
| 14–16     | 3–4   | Vidente + Bruja + Ángel + Caballero (+ Cupido) | 6–9 | opcional |

Con más roles especiales, el pueblo gana información y los lobos necesitan más número. El host
puede ajustarlo; el servidor solo valida que el reparto sea posible.

## 3. Roles

### Lobo

- Conoce a los demás lobos y tiene un **chat privado** durante la noche (#38).
- Cada noche vota en secreto a quién atacar. **Objetivo válido: cualquier jugador vivo**, incluido
  otro lobo o uno mismo.
- El ataque hace **1 punto de daño**.
- Si hay **2 o más lobos**, la ventana de decisión es de **40 s**; con uno solo, la ventana normal
  de rol (20 s).
- Si no hay consenso, el objetivo se elige **al azar entre los más votados**.

### Aldeano

- No tiene acción nocturna.
- Su arma es el voto en el linchamiento.

### Vidente (`seer`)

- Cada noche elige a un jugador y **ve su rol** (el rol base, no los enamoramientos).
- Las vidas son públicas, así que no recibe información extra sobre ellas.
- Puede repetir el mismo objetivo varias noches.
- No ve el cargo de Presidente (es público) ni si alguien es enamorado.

### Bruja (`witch`)

- Tiene **dos pociones de un solo uso**: una de vida y una de muerte.
- Cada noche **ve el nombre de la víctima elegida por los lobos**.
- Puede usar **una** de estas opciones:
  - **Curar**: restaura **1 vida** a cualquier jugador. Aplicada a la víctima de los lobos esa
    misma noche, puede evitar su muerte definitiva.
  - **Envenenar**: hace **1 punto de daño** a cualquier jugador.
  - **No hacer nada**.
- Solo puede usar **una poción por noche** y cada una **una vez por partida**.
- Si los lobos la matan esa misma noche, **no actúa** (#39): la resolución de la Bruja va después
  de la de los lobos.

### Ángel de la Guarda (`guardian`)

- Cada noche protege a un jugador.
- Tiene **3 protecciones por partida**.
- Puede protegerse a sí mismo, pero **no en noches consecutivas**.
- **No puede repetir** el mismo protegido en noches consecutivas.
- La protección anula **el ataque de los lobos y la poción de muerte** de esa noche, aunque haya
  varios ataques de esos tipos contra el mismo objetivo.
- **No** protege del golpe del Caballero ni del linchamiento.
- No sabe si su protección llegó a usarse.

### Caballero de la Espada Oxidada (`knight`)

- Hace las veces de cazador: **al morir de forma definitiva elige a un jugador y le golpea**.
- El golpe hace **1 punto de daño** y puede quitarle la última vida.
- El golpe **se resuelve en la ronda de día**: si muere de noche, se aplica al amanecer; si muere
  linchado, se aplica inmediatamente después del linchamiento.
- No puede golpear a un jugador ya muerto.

### Cupido (`cupid`)

- **Solo la primera noche** elige a **dos enamorados**. Puede elegirse a sí mismo como uno de ellos.
- Los dos enamorados **se reconocen** (saben quién es el otro), pero no ven su rol.
- Si uno **muere de forma definitiva**, el otro **muere de amor al instante**, sin importar sus
  vidas.
- Los enamorados **no pueden votarse entre sí** en el linchamiento.
- Si son de **bandos distintos**, dejan de pertenecer a su bando y pasan a ser **un bando propio**.

### Presidente (cargo, no rol)

- Es opcional y se activa en la configuración de la sala.
- Se elige por **votación popular al empezar la partida**, antes del reparto de roles.
- **Su voto vale doble** en el linchamiento (también en la segunda vuelta).
- Es un cargo **público**: todos saben quién es.
- **Se pierde al morir** y no se vuelve a elegir (el cargo queda vacío).

## 4. Sistema de vidas

- La partida se juega con **0, 1 o 2 vidas** por jugador, decidido por el host (por defecto **1**).
- **Cualquier golpe quita una vida**: ataque de lobos, veneno de la Bruja, golpe del Caballero o
  linchamiento.
- Un jugador con vidas restantes **sobrevive** al golpe; solo muere de forma definitiva al llegar
  a **0**.
- Las **vidas restantes son públicas**: se anuncian las pérdidas.
- Con **0 vidas**, cualquier golpe es muerte definitiva.
- La muerte por amor de los enamorados y la muerte ya consumada **no se pueden evitar** con vidas.
- La poción de vida de la Bruja **restaura 1 vida** (nunca por encima del máximo configurado).

## 5. Flujo de la partida

1. **Elección de Presidente** (si está activado).
2. **Reparto** de roles, una carta por jugador.
3. **Revelación** de la carta propia (10 s).
4. **Noche** (ronda 1: incluye a Cupido).
5. **Amanecer**: anuncio de eventos y muertes.
6. **Debate**.
7. **Votación** y **linchamiento**.
8. Vuelta a la **noche** hasta que se cumpla una condición de victoria.

Todo el estado lo gobierna el servidor; el cliente solo renderiza lo que recibe (#34).

## 6. Noche

### Orden de las acciones

En la primera noche, antes que nada, actúa **Cupido**. Después el orden es siempre el mismo (#39):

1. **Cupido** (solo noche 1).
2. **Vidente**.
3. **Ángel de la Guarda**.
4. **Lobos**.
5. **Bruja**.

Las ventanas de acción de los roles que no están en juego o ya han muerto se saltan.

### Resolución

La noche se resuelve en este orden estricto:

1. **Protección del Ángel**: marca al protegido como inmune a lobos y veneno esa noche.
2. **Ataque de los lobos**: si el objetivo no está protegido, pierde 1 vida.
3. **Poción de la Bruja**: cura (restaura 1 vida) o envenena (quita 1 vida), salvo protección.
4. **Consecuencias**: Caballero si ha muerto, enamorados si uno muere, muertes definitivas.
5. **Comprobación de victoria** tras cada muerte definitiva.

Si un rol no actúa dentro de su ventana, su acción se omite (los lobos no atacan si no hay
objetivo). Si el jugador está desconectado, se comporta igual: su ventana expira y no actúa.

## 7. Día

### Amanecer

1. Primero **se hace de día** y después se anuncian los eventos (#40).
2. Se anuncia cada muerte y cada pérdida de vida (con las vidas restantes).
3. Si **nadie muere ni pierde vidas**, se dice explícitamente: «Esta noche no ha pasado nada».
4. Las **muertes nocturnas no revelan el rol**; el linchamiento sí (ver abajo).

### Debate

- Temporizador configurable, **240 s por defecto** (4 minutos, #41).
- Cada jugador vivo tiene un botón **«Listo para votar»**.
- Si **todos** los vivos conectados lo pulsan, se salta directamente a la votación.

### Votación

- **Voto secreto**: cada jugador ve solo el suyo (#42).
- Pueden votar **todos los vivos**. Abstenerse es válido y no suma a nadie.
- No se puede votar a uno mismo.
- El recuento se revela cuando **han votado todos los vivos conectados** o cuando **expira el
  tiempo** (45 s por defecto).
- Si la opción **«mostrar detalle de votos»** está activa, tras el recuento se muestra **quién votó
  a quién**; si no, solo los totales.
- **Empate**: segunda vuelta **solo entre los empatados**. Si el empate persiste, **nadie muere**
  ese día.
- El voto del Presidente **cuenta doble** en ambas rondas.

### Linchamiento

- El más votado recibe **1 punto de daño**.
- Si le quedan vidas, **sobrevive** y el día termina sin eliminación (**no se revela su rol**).
- Si llega a 0 vidas, **muere y su rol se revela públicamente** (#43).
- Después se aplican las reacciones (Caballero) y las consecuencias (enamorados).

## 8. Victoria

Se comprueba **después de cada muerte definitiva** (#44), en este orden de prioridad:

1. **Enamorados**: si solo quedan vivos los dos enamorados (de bandos distintos), ganan ellos.
2. **Pueblo**: si no queda ningún lobo vivo, gana el pueblo.
3. **Lobos**: si no queda ningún no-lobo vivo, ganan los lobos.

Si en la misma resolución mueren el último lobo y el último no-lobo, **gana el pueblo** (han
eliminado a todos los lobos). La partida termina en ese instante y se revelan todos los roles.

## 9. Casos límite

| Situación | Resolución |
| --------- | ---------- |
| El Ángel protege a la víctima de los lobos | El ataque no hace daño. |
| El Ángel protege a la víctima del veneno | El veneno no hace daño. |
| Lobos y Bruja atacan al mismo objetivo | Dos golpes: puede perder 2 vidas. |
| La Bruja cura a la víctima de los lobos | Restaura 1 vida después del ataque; si no había llegado a 0, recupera la vida perdida. |
| La Bruja es la víctima de los lobos | No actúa esa noche (muere antes de su turno). |
| El Caballero muere por la noche | Elige objetivo al amanecer; el golpe se resuelve antes de las votaciones. |
| El Caballero muere linchado | Elige objetivo inmediatamente después del linchamiento. |
| El Caballero golpea a alguien con vidas | Le quita 1 vida; si llega a 0, muerte definitiva y revelación de rol. |
| Un enamorado muere | El otro muere al instante, sin tiradas de vidas. |
| Los enamorados son del mismo bando | Siguen en su bando; el amor solo añade la muerte vinculada. |
| El Presidente muere | El cargo queda vacío; no hay reelección. |
| Todos se abstienen | Nadie es linchado. |
| Empate en la segunda vuelta | Nadie es linchado. |
| Empate en la elección de Presidente | Segunda vuelta entre empatados; si persiste, **sorteo** entre ellos. |
| Un jugador desconectado tiene que votar o actuar | Se omite su acción; en votación cuenta como abstención. |
| Un jugador muerto intenta actuar o votar | El servidor lo rechaza; los muertos no hablan en el chat público. |
| Mueren todos a la vez | Gana el pueblo si no queda ningún lobo; si no queda nadie, se aplica ese mismo orden. |

## 10. Tiempos por defecto

Los tiempos de rol, debate y votación son configurables por el host dentro de los rangos de #27.

| Fase | Por defecto | Rango |
| ---- | ----------- | ----- |
| Elección de Presidente | 45 s | fijo |
| Revelación de carta | 10 s | fijo |
| Acción de rol (cada uno) | 20 s | 10–120 s |
| Lobos (2+ lobos) | 40 s | máx(rol, 40 s) |
| Amanecer / anuncios | 10 s | fijo |
| Debate | 240 s | 30–600 s |
| Votación | 45 s | 15–180 s |
| Segunda vuelta | 30 s | fijo |
| Caballero al morir | 20 s | fijo |

> **Nota de coherencia:** el código actual usa `debateSeconds = 120` y `roleSeconds = 30` por
> defecto. Este reglamento propone **240** y **20** respectivamente (D1 y D2). Son cambios de una
> línea en `GameSettings.DEFAULT`.

## 11. Desconexiones y abandono

- **Desconectarse no es salir**: el asiento se reserva y el jugador puede volver con `resume_room`
  (#26).
- Mientras está desconectado: no actúa, no vota y no habla; su ventana expira.
- **Salir** (`leave_room`) en el lobby elimina al jugador; si sale el host, la sala se cierra
  (#24).
- **Expulsar** solo está permitido en el lobby y solo al host (#24).
- Una partida empezada **no admite jugadores nuevos** (`game_already_started`).
- Si un jugador no vuelve, la partida continúa sin él; no se le sustituye por bots.

## 12. Chat (avance de la épica 7)

- **Público**: todos los vivos durante el día.
- **Lobos**: solo los lobos, solo de noche.
- **Muertos**: solo los muertos, siempre.
- **Enamorados**: canal privado entre ellos, siempre (si se confirma en #50).
- Los mensajes del narrador son del sistema y no se pueden responder.

## 13. Decisiones abiertas

Cada punto lleva la opción recomendada. Si no dices lo contrario, el motor implementará esa.

| ID | Decisión | Recomendación | Alternativa |
| -- | -------- | ------------- | ----------- |
| D1 | Debate por defecto | **240 s** (coherente con #41) | mantener 120 s |
| D2 | Ventana de acción de rol | **20 s** por defecto | mantener 30 s |
| D3 | ¿Cuándo se elige al Presidente? | **Antes del reparto**, todos votan sin saber roles | después del reparto |
| D4 | Voto doble del Presidente | **También en la segunda vuelta** | solo en la primera |
| D5 | Empate en la segunda vuelta | **Nadie muere** | sorteo entre empatados |
| D6 | Muerte de un enamorado | **Definitiva**, ignora vidas | pierde todas las vidas |
| D7 | Revelación de rol al morir de noche | **No se revela**; solo el linchamiento revela | revelar siempre |
| D8 | Momento del golpe del Caballero | **Al amanecer** si muere de noche; inmediato si muere linchado | elegir de noche y aplicar al amanecer |
| D9 | Protección del Ángel | **Anula todos** los ataques de lobos/veneno de esa noche | solo el primero |
| D10 | Pociones de la Bruja | **Una por noche**, cada una una vez | puede usar las dos la misma noche |
| D11 | Muerte simultánea de último lobo y último aldeano | **Gana el pueblo** | empate |
| D12 | Muerte del Presidente | **Sin reelección**, cargo vacío | nueva elección al día siguiente |
| D13 | Salir/expulsar con la partida empezada | **No permitido**; solo desconexión con asiento reservado | permitir abandono (muere) |

## 14. Trazabilidad

| Regla | Issues |
| ----- | ------ |
| Fases y transiciones | #34 |
| Reparto y secreto | #35 |
| Vidas 0/1/2 | #36 |
| Ventanas y orden nocturno | #37, #39 |
| Voto interno de lobos | #38 |
| Amanecer y anuncios | #40 |
| Debate y botón listo | #41 |
| Votación, abstención y empates | #42 |
| Linchamiento y revelación | #43 |
| Victoria | #44 |
| Vidente | #46 |
| Bruja | #47 |
| Ángel | #48 |
| Caballero | #49 |
| Cupido y enamorados | #50 |
| Presidente | #51 |
| Validación de configuración | #27 |
| Caducidad y reconexión | #25, #26 |
| Chat | #62, #63 |
