import type { SoundName } from './sounds'

type Note = [frequency: number, duration: number, type: OscillatorType]

const SOUNDS: Record<SoundName, Note[]> = {
  turn: [
    [660, 0.09, 'sine'],
    [880, 0.14, 'sine'],
  ],
  phase: [[220, 0.12, 'sine']],
  dawn: [
    [523, 0.12, 'sine'],
    [659, 0.12, 'sine'],
    [784, 0.2, 'sine'],
  ],
  death: [
    [110, 0.24, 'sawtooth'],
    [82, 0.36, 'sawtooth'],
  ],
  hit: [[140, 0.12, 'square']],
  vote: [
    [440, 0.1, 'triangle'],
    [554, 0.14, 'triangle'],
  ],
  calm: [
    [392, 0.12, 'sine'],
    [523, 0.16, 'sine'],
  ],
}

type AudioWindow = Window & { webkitAudioContext?: typeof AudioContext }

let context: AudioContext | null = null

function getContext(): AudioContext | null {
  if (typeof window === 'undefined') {
    return null
  }
  const Constructor = window.AudioContext ?? (window as AudioWindow).webkitAudioContext
  if (!Constructor) {
    return null
  }
  context ??= new Constructor()
  if (context.state === 'suspended') {
    void context.resume()
  }
  return context
}

/** Reproduce un sonido sintetizado; si el navegador no lo permite, no falla. */
export function playSound(name: SoundName) {
  try {
    const audio = getContext()
    if (!audio) {
      return
    }

    let offset = 0
    for (const [frequency, duration, type] of SOUNDS[name]) {
      const oscillator = audio.createOscillator()
      const gain = audio.createGain()
      oscillator.type = type
      oscillator.frequency.value = frequency
      gain.gain.setValueAtTime(0.0001, audio.currentTime + offset)
      gain.gain.exponentialRampToValueAtTime(0.05, audio.currentTime + offset + 0.01)
      gain.gain.exponentialRampToValueAtTime(0.0001, audio.currentTime + offset + duration)
      oscillator.connect(gain)
      gain.connect(audio.destination)
      oscillator.start(audio.currentTime + offset)
      oscillator.stop(audio.currentTime + offset + duration)
      offset += duration * 0.9
    }
  } catch {
    // El audio es opcional: un navegador sin Web Audio no debe romper la partida.
  }
}

/** Vibración móvil, también opcional. */
export function vibrate(pattern: number | number[]) {
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(pattern)
    }
  } catch {
    // Sin soporte de vibración no pasa nada.
  }
}
