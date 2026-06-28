import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'
import { useAuth } from '../auth.jsx'

const fmtData = (iso) => {
  try {
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' })
  } catch {
    return ''
  }
}

export default function Campanhas() {
  const { user } = useAuth()
  const nav = useNavigate()
  const [orgId, setOrgId] = useState(null)
  const [lista, setLista] = useState([])
  const [nome, setNome] = useState('')
  const [codigo, setCodigo] = useState('')
  const [erro, setErro] = useState(null)

  // Junta as campanhas da minha org + as que entrei por convite (de outras orgs).
  const carregar = async (oid) => {
    const [orgC, minhas] = await Promise.all([
      api(`/api/organizacoes/${oid}/campanhas`).catch(() => []),
      api('/api/campanhas/minhas').catch(() => []),
    ])
    const porId = new Map()
    ;[...(orgC || []), ...(minhas || [])].forEach((c) => porId.set(c.id, c))
    setLista([...porId.values()])
  }

  useEffect(() => {
    (async () => {
      const oid = await obterOrgId()
      setOrgId(oid)
      await carregar(oid)
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

  async function entrar(e) {
    e.preventDefault()
    if (!codigo.trim()) return
    setErro(null)
    try {
      const m = await api(`/api/campanhas/entrar/${codigo.trim()}`, { method: 'POST', body: { usuarioId: user?.id } })
      setCodigo('')
      nav(`/campanhas/${m.campanhaId}`)
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

      <div className="card">
        <h2>Entrar com código</h2>
        <p className="muted" style={{ marginTop: -4, fontSize: '.84rem' }}>
          Cole o código de convite que o mestre gerou (na campanha → Gerar convite).
        </p>
        <form onSubmit={entrar} className="row">
          <div style={{ flex: 1 }}>
            <label>Código do convite</label>
            <input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="Ex.: AB12CD" />
          </div>
          <button className="ghost" style={{ alignSelf: 'end' }}>Entrar</button>
        </form>
      </div>
    </>
  )
}
