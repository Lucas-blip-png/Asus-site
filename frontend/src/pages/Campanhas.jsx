import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'

const fmtData = (iso) => {
  try {
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' })
  } catch {
    return ''
  }
}

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
      <div className="page-head">
        <h1>Campanhas</h1>
        <span className="count-badge"><b>{lista.length}</b></span>
      </div>
      {erro && <p className="error">{erro}</p>}
      <div className="grid">
        {lista.map((c) => (
          <div key={c.id} className="cover-card">
            <div
              className={`cover${c.capaAssetId ? '' : ' placeholder'}`}
              style={c.capaAssetId ? { backgroundImage: `url(/api/assets/${c.capaAssetId}/conteudo)` } : undefined}
            >
              {!c.capaAssetId && (c.nome || '?').charAt(0).toUpperCase()}
              {c.criadoEm && <span className="chip right">{fmtData(c.criadoEm)}</span>}
            </div>
            <div className="body">
              <div className="name">{c.nome}</div>
              <div className="muted" style={{ fontSize: '.8rem' }}>{c.descricao || 'Sem descrição'}</div>
              <div className="foot">
                <Link to={`/campanhas/${c.id}`}><button className="mini">Acessar</button></Link>
                <Link to={`/campanhas/${c.id}/escudo`} className="tag">Escudo</Link>
              </div>
            </div>
          </div>
        ))}
        {lista.length === 0 && <p className="muted">Nenhuma campanha ainda.</p>}
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
