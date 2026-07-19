import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { api } from '../api.js'

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const cls = ({ isActive }) => (isActive ? 'ativo' : undefined)
  const [naoLidas, setNaoLidas] = useState(0)
  const [emailMsg, setEmailMsg] = useState(null)
  const [ocultarAviso, setOcultarAviso] = useState(false)

  async function reenviarVerificacao() {
    setEmailMsg(null)
    try {
      await api('/api/auth/reenviar-verificacao', { method: 'POST' })
      setEmailMsg('E-mail de confirmação reenviado — confira sua caixa de entrada.')
    } catch (e) { setEmailMsg(e.message) }
  }

  useEffect(() => {
    if (!user?.id) return undefined
    let ativo = true
    const buscar = () =>
      api(`/api/me/notificacoes/nao-lidas?usuarioId=${user.id}`)
        .then((d) => { if (ativo) setNaoLidas(d?.total || 0) })
        .catch(() => {})
    buscar()
    const t = setInterval(buscar, 30000)
    // A tela de Notificações dispara este evento ao marcar tudo como lido: zera na hora.
    const zerar = () => setNaoLidas(0)
    window.addEventListener('notificacoes-lidas', zerar)
    return () => { ativo = false; clearInterval(t); window.removeEventListener('notificacoes-lidas', zerar) }
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
      {user && !user.emailVerificado && !ocultarAviso && (
        <div style={{
          background: 'rgba(224,166,74,.14)', borderBottom: '1px solid rgba(224,166,74,.3)',
          padding: '8px 16px', display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap',
        }}>
          <span style={{ fontSize: '.88rem' }}>✉️ Confirme seu e-mail para proteger sua conta.</span>
          {emailMsg && <span className="muted" style={{ fontSize: '.82rem' }}>{emailMsg}</span>}
          <div style={{ flex: 1 }} />
          <button className="ghost mini" onClick={reenviarVerificacao}>Reenviar e-mail</button>
          <button className="ghost mini" onClick={() => setOcultarAviso(true)}>Agora não</button>
        </div>
      )}
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
