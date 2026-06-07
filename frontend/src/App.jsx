import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Organizacao from './pages/Organizacao.jsx'
import Personagem from './pages/Personagem.jsx'
import Campanha from './pages/Campanha.jsx'
import Escudo from './pages/Escudo.jsx'
import Overlay from './pages/Overlay.jsx'

function Protected({ children }) {
  const { user, carregando } = useAuth()
  if (carregando) return <div className="center">Carregando…</div>
  return user ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      {/* Overlay OBS e publico (consome WebSocket) — Fase 9 */}
      <Route path="/overlay/:campanhaId" element={<Overlay />} />
      <Route
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/orgs/:orgId" element={<Organizacao />} />
        <Route path="/personagens/:id" element={<Personagem />} />
        <Route path="/campanhas/:id" element={<Campanha />} />
        <Route path="/campanhas/:id/escudo" element={<Escudo />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
