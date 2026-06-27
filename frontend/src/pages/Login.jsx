import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { api } from '../api.js'

export default function Login() {
  const { login, registrar } = useAuth()
  const nav = useNavigate()
  const [modo, setModo] = useState('login')
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('dev@asus.local')
  const [senha, setSenha] = useState('dev12345')
  const [erro, setErro] = useState(null)
  const [carregando, setCarregando] = useState(false)
  const [googleOn, setGoogleOn] = useState(false)

  // Retorno do login social (Google): os tokens chegam no fragmento da URL.
  useEffect(() => {
    const hash = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : ''
    const params = new URLSearchParams(hash)
    const at = params.get('access_token')
    const rt = params.get('refresh_token')
    if (at && rt) {
      localStorage.setItem('accessToken', at)
      localStorage.setItem('refreshToken', rt)
      window.location.replace('/') // recarrega para o AuthProvider ler o token
      return
    }
    if (params.get('erro')) setErro('Falha no login social: ' + params.get('erro'))
    api('/api/auth/config', { auth: false }).then((c) => setGoogleOn(!!c?.googleOAuth)).catch(() => {})
  }, [])

  async function submit(e) {
    e.preventDefault()
    setErro(null)
    setCarregando(true)
    try {
      if (modo === 'login') await login(email, senha)
      else await registrar(nome, email, senha)
      nav('/')
    } catch (ex) {
      setErro(ex.message)
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="container" style={{ maxWidth: 380 }}>
      <h1 className="brand" style={{ textAlign: 'center', justifyContent: 'center' }}>ASUS RPG</h1>
      <p className="muted" style={{ textAlign: 'center', marginTop: -6, marginBottom: 18 }}>
        Plataforma de RPG de mesa
      </p>
      <div className="card">
        <h2>{modo === 'login' ? 'Entrar' : 'Criar conta'}</h2>
        <form onSubmit={submit}>
          {modo === 'registro' && (
            <>
              <label>Nome</label>
              <input value={nome} onChange={(e) => setNome(e.target.value)} required />
            </>
          )}
          <label>E-mail</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <label>Senha</label>
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
          {erro && <p className="error">{erro}</p>}
          <div className="row" style={{ marginTop: 14 }}>
            <button disabled={carregando} type="submit">
              {modo === 'login' ? 'Entrar' : 'Registrar'}
            </button>
            <button
              type="button"
              className="ghost"
              onClick={() => setModo(modo === 'login' ? 'registro' : 'login')}
            >
              {modo === 'login' ? 'Criar conta' : 'Já tenho conta'}
            </button>
          </div>
        </form>
        {googleOn && (
          <>
            <div className="ou-sep"><span>ou</span></div>
            <a className="btn-google" href="/oauth2/authorization/google">
              <span className="g">G</span> Entrar com Google
            </a>
          </>
        )}
      </div>
      <p className="muted" style={{ textAlign: 'center', fontSize: '.8rem' }}>
        Dev: dev@asus.local / dev12345
      </p>
    </div>
  )
}
