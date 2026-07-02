import { useEffect, useState } from 'react'
import { api } from '../api.js'

const VAZIA = {
  nome: '', categoria: 'Humanoide', rank: '', imagemUrl: '',
  especie: '', tipo: '', nivel: 0, pv: 0, pm: 0, pe: 0, defesa: 0, descricao: '',
}

// "Tier" do rank para colorir a etiqueta (SSS/SS/S alto, A médio-alto, B, C…).
function rankTier(rank) {
  const r = (rank || '').toUpperCase()
  if (r.includes('SSS')) return 'sss'
  if (r.includes('SS')) return 'ss'
  if (r.startsWith('S')) return 's'
  if (r.startsWith('A')) return 'a'
  if (r.startsWith('B')) return 'b'
  if (r.startsWith('C')) return 'c'
  return 'x'
}

function CriaturaRow({ c, onDelete }) {
  const [open, setOpen] = useState(false)
  const temStats = c.pv || c.pm || c.pe || c.defesa || c.nivel
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <div className="bestia-av"
          style={c.imagemUrl ? { backgroundImage: `url(${c.imagemUrl})` } : undefined}>
          {!c.imagemUrl && (c.nome || '?').charAt(0).toUpperCase()}
        </div>
        <b className="nm">{c.nome}</b>
        <span className="sub">{c.categoria || c.tipo || 'Criatura'}</span>
        <div className="spacer" />
        {c.rank && <span className={`rank-badge r-${rankTier(c.rank)}`}>{c.rank}</span>}
        {c.oficial
          ? <span className="tag">Oficial</span>
          : <button className="ghost mini" title="Remover"
              onClick={(e) => { e.stopPropagation(); onDelete(c.id) }}>✕</button>}
      </div>
      {open && (
        <div className="cris-body">
          {c.imagemUrl && (
            <img src={c.imagemUrl} alt={c.nome}
              style={{ width: '100%', maxWidth: 360, borderRadius: 10, marginBottom: 10, display: 'block' }} />
          )}
          <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
            {c.rank && <span className="tag">Rank {c.rank}</span>}
            {c.categoria && <span className="tag">{c.categoria}</span>}
            {temStats ? (
              <>
                {c.nivel ? <span className="tag">Nv {c.nivel}</span> : null}
                {c.pv ? <span className="tag">PV {c.pv}</span> : null}
                {c.pm ? <span className="tag">PM {c.pm}</span> : null}
                {c.pe ? <span className="tag">PE {c.pe}</span> : null}
                {c.defesa ? <span className="tag">Defesa {c.defesa}</span> : null}
              </>
            ) : null}
          </div>
          {c.descricao && <p className="muted" style={{ marginTop: 8, fontSize: '.84rem' }}>{c.descricao}</p>}
        </div>
      )}
    </div>
  )
}

export default function Bestiario() {
  const [busca, setBusca] = useState('')
  const [criaturas, setCriaturas] = useState([])
  const [nova, setNova] = useState(VAZIA)
  const [erro, setErro] = useState(null)
  const [abrirForm, setAbrirForm] = useState(false)

  const carregar = () => api('/api/bestiario').then(setCriaturas).catch((e) => setErro(e.message))
  useEffect(() => { carregar() }, [])

  async function criar() {
    if (!nova.nome.trim()) return
    try {
      await api('/api/bestiario', {
        method: 'POST',
        body: {
          ...nova,
          nivel: Number(nova.nivel) || 0,
          pv: Number(nova.pv) || 0, pm: Number(nova.pm) || 0,
          pe: Number(nova.pe) || 0, defesa: Number(nova.defesa) || 0,
        },
      })
      setNova(VAZIA)
      setAbrirForm(false)
      carregar()
    } catch (e) { setErro(e.message) }
  }
  async function apagar(id) {
    try { await api(`/api/bestiario/${id}`, { method: 'DELETE' }); carregar() } catch (e) { setErro(e.message) }
  }

  const filtradas = criaturas.filter((c) =>
    (c.nome + ' ' + (c.categoria || '') + ' ' + (c.rank || '')).toLowerCase().includes(busca.toLowerCase()))

  // Agrupa por categoria (mantém a ordem de chegada das categorias).
  const agrupadas = filtradas.reduce((acc, c) => {
    const cat = c.categoria || c.tipo || 'Outras'
    ;(acc[cat] = acc[cat] || []).push(c)
    return acc
  }, {})

  return (
    <>
      <div className="page-head">
        <h1>Bestiário</h1>
        <span className="count-badge"><b>{criaturas.length}</b></span>
        <div className="spacer" />
        <button onClick={() => setAbrirForm((v) => !v)}>{abrirForm ? 'Fechar' : '+ Nova criatura'}</button>
      </div>
      {erro && <p className="error">{erro}</p>}

      <div className="search-wrap">
        <span className="ic">🔍</span>
        <input placeholder="Buscar por nome, categoria ou rank" value={busca} onChange={(e) => setBusca(e.target.value)} />
      </div>

      {abrirForm && (
        <div className="card add-form" style={{ marginBottom: 16, alignItems: 'flex-end' }}>
          <div style={{ flex: '1 1 160px' }}>
            <label>Nome</label>
            <input value={nova.nome} onChange={(e) => setNova((s) => ({ ...s, nome: e.target.value }))} />
          </div>
          <div style={{ width: 140 }}>
            <label>Categoria</label>
            <input placeholder="Humanoide / Bestial…" value={nova.categoria}
              onChange={(e) => setNova((s) => ({ ...s, categoria: e.target.value }))} />
          </div>
          <div style={{ width: 90 }}>
            <label>Rank</label>
            <input placeholder="S+" value={nova.rank}
              onChange={(e) => setNova((s) => ({ ...s, rank: e.target.value }))} />
          </div>
          <div style={{ flex: '1 1 180px' }}>
            <label>Imagem (URL)</label>
            <input placeholder="https://…" value={nova.imagemUrl}
              onChange={(e) => setNova((s) => ({ ...s, imagemUrl: e.target.value }))} />
          </div>
          <div style={{ width: 70 }}>
            <label>PV</label>
            <input type="number" value={nova.pv} onChange={(e) => setNova((s) => ({ ...s, pv: e.target.value }))} />
          </div>
          <div style={{ width: 80 }}>
            <label>Defesa</label>
            <input type="number" value={nova.defesa} onChange={(e) => setNova((s) => ({ ...s, defesa: e.target.value }))} />
          </div>
          <div style={{ flex: '1 1 100%' }}>
            <label>Descrição</label>
            <textarea value={nova.descricao} onChange={(e) => setNova((s) => ({ ...s, descricao: e.target.value }))} />
          </div>
          <button className="mini" onClick={criar}>Salvar</button>
        </div>
      )}

      {!filtradas.length && <div className="card muted">Nenhuma criatura encontrada.</div>}
      {Object.entries(agrupadas).map(([cat, lista]) => (
        <div key={cat}>
          <h2 style={{ marginTop: 18 }}>
            {cat} <span className="muted" style={{ fontSize: '.8rem', fontWeight: 400 }}>({lista.length})</span>
          </h2>
          <div className="cris-list">
            {lista.map((c) => <CriaturaRow key={c.id} c={c} onDelete={apagar} />)}
          </div>
        </div>
      ))}
    </>
  )
}
