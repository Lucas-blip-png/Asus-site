import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

export default function Dashboard() {
  const [orgs, setOrgs] = useState([])
  const [nome, setNome] = useState('')
  const [slug, setSlug] = useState('')
  const [erro, setErro] = useState(null)

  const carregar = () => api('/api/organizacoes').then(setOrgs).catch((e) => setErro(e.message))
  useEffect(() => {
    carregar()
  }, [])

  async function criar(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api('/api/organizacoes', { method: 'POST', body: { nome, slug } })
      setNome('')
      setSlug('')
      carregar()
    } catch (ex) {
      setErro(ex.message)
    }
  }

  return (
    <>
      <h1>Organizações</h1>
      <div className="grid">
        {orgs.map((o) => (
          <Link key={o.id} to={`/orgs/${o.id}`} className="card">
            <h2>{o.nome}</h2>
            <div className="muted">/{o.slug}</div>
            <span className="tag">{o.plano}</span>
          </Link>
        ))}
      </div>
      <div className="card">
        <h2>Nova organização</h2>
        <form onSubmit={criar} className="row">
          <div style={{ flex: 1 }}>
            <label>Nome</label>
            <input value={nome} onChange={(e) => setNome(e.target.value)} required />
          </div>
          <div style={{ flex: 1 }}>
            <label>Slug</label>
            <input value={slug} onChange={(e) => setSlug(e.target.value)} required />
          </div>
          <button type="submit" style={{ alignSelf: 'end' }}>
            Criar
          </button>
        </form>
        {erro && <p className="error">{erro}</p>}
      </div>
    </>
  )
}
