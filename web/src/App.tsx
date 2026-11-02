import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { GameScreen } from './screens/GameScreen'
import { HomeScreen } from './screens/HomeScreen'
import { LobbyScreen } from './screens/LobbyScreen'
import { SessionProvider } from './session/SessionProvider'

export default function App() {
  return (
    <SessionProvider>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomeScreen />} />
          <Route path="/lobby" element={<LobbyScreen />} />
          <Route path="/game" element={<GameScreen />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </SessionProvider>
  )
}
