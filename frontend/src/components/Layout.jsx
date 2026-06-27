import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { api } from '../api.js'

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const cls = ({ isActive }) => (isActive ? 'ativo' : undefined)
  const [naoLidas, setNaoLidas] = useState(0)

  useEffect(() => {
    if (!user?.id) return undefined
    let ativo = true
    const buscar = () =>
      api(`/api/me/notificacoes/nao-lidas?usuarioId=${user.id}`)
        .then((d) => { if (ativo) setNaoLidas(d?.total || 0) })
        .catch(() => {})
    buscar()
    const t = setInterval(buscar, 30000)
    return () => { ativo = false; clearInterval(t) }
  }, [user?.id])

  return (
    <div>
      <header className="topbar">
        <Link to="/" className="brand">
          <span className="brand-mark">A</span>ASUS
        </Link>
        <nav className="topnav">
          <NavLink to="/personagens" className={cls}>Personagens</NavLink>
          <NavLink to="/campanhas" className={cls}>Campanhas</NavLink>
          <NavLink to="/bestiario" className={cls}>Bestiário</NavLink>
          <NavLink to="/marketplace" className={cls}>Marketplace</NavLink>
          <NavLink to="/templates" className={cls}>Templates</NavLink>
          <NavLink to="/livros" className={cls}>Livros</NavLink>
        </nav>
        <div className="topbar-right">
          <NavLink to="/notificacoes" className="bell" title="Notificações">
            🔔{naoLidas > 0 && <span className="badge">{naoLidas > 9 ? '9+' : naoLidas}</span>}
          </NavLink>
          <Link to="/conta" className="user-chip" title="Minha conta">
            <span className="dot">{(user?.nome || '?').charAt(0).toUpperCase()}</span>
            {user?.nome}
          </Link>
          <button className="ghost mini" title="Sair" onClick={() => { logout(); nav('/login') }}>Sair</button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
