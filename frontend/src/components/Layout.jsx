import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const cls = ({ isActive }) => (isActive ? 'ativo' : undefined)

  return (
    <div>
      <header className="topbar">
        <Link to="/" className="brand">ASUS</Link>
        <nav className="topnav">
          <NavLink to="/personagens" className={cls}>Personagens</NavLink>
          <NavLink to="/campanhas" className={cls}>Campanhas</NavLink>
          <NavLink to="/bestiario" className={cls}>Bestiário</NavLink>
          <NavLink to="/livros" className={cls}>Livros</NavLink>
        </nav>
        <div className="spacer" />
        <div
          className="user-chip"
          title="Sair"
          onClick={() => {
            logout()
            nav('/login')
          }}
        >
          <span className="dot">{(user?.nome || '?').charAt(0).toUpperCase()}</span>
          {user?.nome}
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
