import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'

export default function Campanhas() {
  const [orgId, setOrgId] = useState(null)
  const [lista, setLista] = useState([])
  const [nome, setNome] = useState('')
  const [erro, setErro] = useState(null)

  const carregar = (oid) => api(`/api/organizacoes/${oid}/campanhas`).then(setLista)

  useEffect(() => {
    (async () => {
      const oid = await obterOrgId()
      setOrgId(oid)
      carregar(oid)
    })().catch((e) => setErro(e.message))
  }, [])

  async function criar(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api(`/api/organizacoes/${orgId}/campanhas`, { method: 'POST', body: { nome } })
      setNome('')
      carregar(orgId)
    } catch (ex) {
      setErro(ex.message)
    }
  }

  return (
    <>
      <div className="row">
        <h1>Campanhas</h1>
        <span className="muted">{lista.length}</span>
      </div>
      {erro && <p className="error">{erro}</p>}
      <div className="grid">
        {lista.map((c) => (
          <div key={c.id} className="card">
            <b>{c.nome}</b>
            <div className="muted">{c.descricao || 'Sem descrição'}</div>
            <div className="row" style={{ marginTop: 10 }}>
              <Link to={`/campanhas/${c.id}`}><button className="mini">Acessar</button></Link>
              <Link to={`/campanhas/${c.id}/escudo`} className="tag">Escudo</Link>
            </div>
          </div>
        ))}
      </div>
      <div className="card">
        <h2>Nova campanha</h2>
        <form onSubmit={criar} className="row">
          <div style={{ flex: 1 }}>
            <label>Nome</label>
            <input value={nome} onChange={(e) => setNome(e.target.value)} required />
          </div>
          <button style={{ alignSelf: 'end' }}>Criar</button>
        </form>
      </div>
    </>
  )
}
