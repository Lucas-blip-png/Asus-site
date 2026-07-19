import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'

// Página pública aberta pelo link do e-mail de verificação: confirma o e-mail pelo token.
export default function VerificarEmail() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [estado, setEstado] = useState('verificando') // 'verificando' | 'ok' | 'erro'
  const [erro, setErro] = useState(null)
  const feito = useRef(false)

  useEffect(() => {
    if (feito.current) return // evita dupla chamada (StrictMode)
    feito.current = true
    if (!token) { setEstado('erro'); setErro('Link inválido (sem token).'); return }
    api('/api/auth/verificar-email', { method: 'POST', auth: false, body: { token } })
      .then(() => setEstado('ok'))
      .catch((e) => { setEstado('erro'); setErro(e.message) })
  }, [token])

  return (
    <div className="container" style={{ maxWidth: 380 }}>
      <h1 className="brand" style={{ textAlign: 'center', justifyContent: 'center' }}>ASUS RPG</h1>
      <div className="card">
        <h2>Verificação de e-mail</h2>
        {estado === 'verificando' && <p className="muted">Confirmando seu e-mail…</p>}
        {estado === 'ok' && <p className="ok">E-mail confirmado com sucesso! 🎉</p>}
        {estado === 'erro' && <p className="error">{erro}</p>}
        <Link to="/"><button style={{ marginTop: 10 }}>Ir para o app</button></Link>
      </div>
    </div>
  )
}
