import { Route, Routes } from 'react-router-dom'

function Home() {
  return (
    <main className="flex min-h-full flex-col items-center justify-center gap-6 px-6 text-center">
      <h1 className="text-5xl font-semibold tracking-tight">Lupus 🐺</h1>
      <p className="max-w-md text-parchment/75">
        El juego de los lobos para jugar con amigos desde el navegador.
      </p>
      <span className="rounded-full border border-night-700 px-4 py-1 text-sm text-parchment/60">
        v0.1 · Fundaciones
      </span>
    </main>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
    </Routes>
  )
}
