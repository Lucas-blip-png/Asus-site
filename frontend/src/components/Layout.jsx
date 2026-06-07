import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  return (
    <div>
      <header className="topbar">
        <Link to="/" className="brand">ASUS RPG</Link>
        <div className="spacer" />
        <span className="muted">{user?.nome}</span>
        <button
          className="ghost"
          onClick={() => {
            logout()
            nav('/login')
          }}
        >
          Sair
        </button>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
