import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Home from './pages/Home.jsx'
import Personagens from './pages/Personagens.jsx'
import Ficha from './pages/Ficha.jsx'
import Campanhas from './pages/Campanhas.jsx'
import Campanha from './pages/Campanha.jsx'
import Escudo from './pages/Escudo.jsx'
import Bestiario from './pages/Bestiario.jsx'
import Livros from './pages/Livros.jsx'
import Overlay from './pages/Overlay.jsx'
import Notificacoes from './pages/Notificacoes.jsx'
import Marketplace from './pages/Marketplace.jsx'
import Templates from './pages/Templates.jsx'
import Conta from './pages/Conta.jsx'

function Protected({ children }) {
  const { user, carregando } = useAuth()
  if (carregando) return <div className="center">Carregando…</div>
  return user ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      {/* Overlay OBS e publico (consome WebSocket) */}
      <Route path="/overlay/:campanhaId" element={<Overlay />} />
      <Route
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route path="/" element={<Home />} />
        <Route path="/personagens" element={<Personagens />} />
        <Route path="/personagens/:id" element={<Ficha />} />
        <Route path="/campanhas" element={<Campanhas />} />
        <Route path="/campanhas/:id" element={<Campanha />} />
        <Route path="/campanhas/:id/escudo" element={<Escudo />} />
        <Route path="/bestiario" element={<Bestiario />} />
        <Route path="/marketplace" element={<Marketplace />} />
        <Route path="/templates" element={<Templates />} />
        <Route path="/notificacoes" element={<Notificacoes />} />
        <Route path="/conta" element={<Conta />} />
        <Route path="/livros" element={<Livros />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
