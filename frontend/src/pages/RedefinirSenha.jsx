import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'

// Página pública do link "esqueci minha senha": define uma senha nova pelo token da URL.
export default function RedefinirSenha() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [novaSenha, setNovaSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [ok, setOk] = useState(false)
  const [carregando, setCarregando] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setErro(null)
    if (novaSenha.length < 6) { setErro('A nova senha deve ter ao menos 6 caracteres.'); return }
    setCarregando(true)
    try {
      await api('/api/auth/redefinir-senha', { method: 'POST', auth: false, body: { token, novaSenha } })
      setOk(true)
    } catch (ex) {
      setErro(ex.message)
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="container" style={{ maxWidth: 380 }}>
      <h1 className="brand" style={{ textAlign: 'center', justifyContent: 'center' }}>ASUS RPG</h1>
      <div className="card">
        <h2>Nova senha</h2>
        {!token && <p className="error">Link inválido (sem token).</p>}
        {ok ? (
          <>
            <p className="ok">Senha redefinida com sucesso!</p>
            <Link to="/login"><button style={{ marginTop: 8 }}>Ir para o login</button></Link>
          </>
        ) : (
          <form onSubmit={submit}>
            <label>Nova senha (mín. 6)</label>
            <input type="password" value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} required />
            {erro && <p className="error">{erro}</p>}
            <div className="row" style={{ marginTop: 14 }}>
              <button disabled={carregando || !token} type="submit">Salvar nova senha</button>
              <Link to="/login"><button type="button" className="ghost">Cancelar</button></Link>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
