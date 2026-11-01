import { Link, Outlet } from 'react-router-dom'

/** Estructura visual comun: cabecera, contenido y pie. */
export function AppLayout() {
  return (
    <div className="flex min-h-full flex-col bg-night-950 text-parchment">
      <header className="border-b border-night-800/80">
        <div className="mx-auto flex w-full max-w-3xl items-center justify-between px-6 py-4">
          <Link to="/" className="text-lg font-semibold tracking-tight">
            <h1>Lupus 🐺</h1>
          </Link>
          <span className="rounded-full border border-night-700 px-3 py-1 text-xs text-parchment/60">
            v0.2 · Salas y lobby
          </span>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col justify-center px-6 py-10">
        <Outlet />
      </main>

      <footer className="border-t border-night-800/80 px-6 py-4 text-center text-xs text-parchment/40">
        El pueblo duerme. El servidor decide.
      </footer>
    </div>
  )
}
