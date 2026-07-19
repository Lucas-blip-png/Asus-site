import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { api } from '../api.js'

export default function Login() {
  const { login, registrar } = useAuth()
  const nav = useNavigate()
  const [modo, setModo] = useState('login') // 'login' | 'registro' | 'esqueci'
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [msg, setMsg] = useState(null)
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

  function trocarModo(novo) {
    setModo(novo); setErro(null); setMsg(null)
  }

  async function submit(e) {
    e.preventDefault()
    setErro(null); setMsg(null)
    setCarregando(true)
    try {
      if (modo === 'login') { await login(email, senha); nav('/') }
      else if (modo === 'registro') { await registrar(nome, email, senha); nav('/') }
      else {
        // esqueci a senha: dispara o link (resposta é sempre igual, não revela se o e-mail existe)
        const r = await api('/api/auth/esqueci-senha', { method: 'POST', auth: false, body: { email } })
        setMsg(r?.status || 'Se o e-mail existir, enviamos um link para redefinir a senha.')
      }
    } catch (ex) {
      setErro(ex.message)
    } finally {
      setCarregando(false)
    }
  }

  const titulo = modo === 'login' ? 'Entrar' : modo === 'registro' ? 'Criar conta' : 'Recuperar senha'

  return (
    <div className="container" style={{ maxWidth: 380 }}>
      <h1 className="brand" style={{ textAlign: 'center', justifyContent: 'center' }}>ASUS RPG</h1>
      <p className="muted" style={{ textAlign: 'center', marginTop: -6, marginBottom: 18 }}>
        Plataforma de RPG de mesa
      </p>
      <div className="card">
        <h2>{titulo}</h2>
        {modo === 'esqueci' && (
          <p className="muted" style={{ marginTop: -4, fontSize: '.86rem' }}>
            Informe seu e-mail e enviaremos um link para criar uma nova senha.
          </p>
        )}
        <form onSubmit={submit}>
          {modo === 'registro' && (
            <>
              <label>Nome</label>
              <input value={nome} onChange={(e) => setNome(e.target.value)} required />
            </>
          )}
          <label>E-mail</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          {modo !== 'esqueci' && (
            <>
              <label>Senha</label>
              <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
            </>
          )}
          {erro && <p className="error">{erro}</p>}
          {msg && <p className="ok">{msg}</p>}
          <div className="row" style={{ marginTop: 14 }}>
            <button disabled={carregando} type="submit">
              {modo === 'login' ? 'Entrar' : modo === 'registro' ? 'Registrar' : 'Enviar link'}
            </button>
            {modo === 'esqueci' ? (
              <button type="button" className="ghost" onClick={() => trocarModo('login')}>Voltar</button>
            ) : (
              <button type="button" className="ghost" onClick={() => trocarModo(modo === 'login' ? 'registro' : 'login')}>
                {modo === 'login' ? 'Criar conta' : 'Já tenho conta'}
              </button>
            )}
          </div>
        </form>
        {modo === 'login' && (
          <button type="button" onClick={() => trocarModo('esqueci')}
            style={{ marginTop: 10, background: 'none', border: 'none', padding: 0, cursor: 'pointer',
              color: 'var(--gold, #e0b64a)', textDecoration: 'underline', fontSize: '.84rem' }}>
            Esqueci minha senha
          </button>
        )}
        {googleOn && modo !== 'esqueci' && (
          <>
            <div className="ou-sep"><span>ou</span></div>
            <a className="btn-google" href="/oauth2/authorization/google">
              <span className="g">G</span> Entrar com Google
            </a>
          </>
        )}
      </div>
    </div>
  )
}
