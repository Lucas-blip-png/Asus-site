import { useEffect, useState } from 'react'
import { api } from '../api.js'

const VAZIA = { nome: '', especie: '', tipo: '', nivel: 1, pv: 0, pm: 0, pe: 0, defesa: 10, descricao: '' }

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
          nivel: Number(nova.nivel) || 1,
          pv: Number(nova.pv) || 0, pm: Number(nova.pm) || 0,
          pe: Number(nova.pe) || 0, defesa: Number(nova.defesa) || 10,
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
    (c.nome + ' ' + (c.especie || '') + ' ' + (c.tipo || '')).toLowerCase().includes(busca.toLowerCase()))

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
        <input placeholder="Buscar criatura" value={busca} onChange={(e) => setBusca(e.target.value)} />
      </div>

      {abrirForm && (
        <div className="card add-form" style={{ marginBottom: 16 }}>
          <input placeholder="Nome" value={nova.nome}
            onChange={(e) => setNova((s) => ({ ...s, nome: e.target.value }))} />
          <input placeholder="Espécie" value={nova.especie}
            onChange={(e) => setNova((s) => ({ ...s, especie: e.target.value }))} />
          <input placeholder="Tipo" value={nova.tipo}
            onChange={(e) => setNova((s) => ({ ...s, tipo: e.target.value }))} />
          <input type="number" placeholder="Nível" style={{ maxWidth: 90 }} value={nova.nivel}
            onChange={(e) => setNova((s) => ({ ...s, nivel: e.target.value }))} />
          <input type="number" placeholder="PV" style={{ maxWidth: 80 }} value={nova.pv}
            onChange={(e) => setNova((s) => ({ ...s, pv: e.target.value }))} />
          <input type="number" placeholder="Defesa" style={{ maxWidth: 90 }} value={nova.defesa}
            onChange={(e) => setNova((s) => ({ ...s, defesa: e.target.value }))} />
          <input placeholder="Descrição" style={{ flexBasis: '100%' }} value={nova.descricao}
            onChange={(e) => setNova((s) => ({ ...s, descricao: e.target.value }))} />
          <button className="mini" onClick={criar}>Salvar</button>
        </div>
      )}

      {!filtradas.length && <div className="card muted">Nenhuma criatura. Crie a primeira com “+ Nova criatura”.</div>}
      <div className="grid">
        {filtradas.map((c) => (
          <div key={c.id} className="card">
            <div className="row">
              <b style={{ fontSize: '1.02rem' }}>{c.nome}</b>
              <div className="spacer" />
              <span className="tag gold">Nv {c.nivel}</span>
            </div>
            <div className="muted" style={{ margin: '4px 0' }}>
              {[c.especie, c.tipo].filter(Boolean).join(' · ') || 'Criatura'}
            </div>
            <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
              <span className="tag">PV {c.pv}</span>
              {c.pm ? <span className="tag">PM {c.pm}</span> : null}
              {c.pe ? <span className="tag">PE {c.pe}</span> : null}
              <span className="tag">Defesa {c.defesa}</span>
            </div>
            {c.descricao && <p className="muted" style={{ marginTop: 6, fontSize: '.82rem' }}>{c.descricao}</p>}
            <div className="row" style={{ marginTop: 8 }}>
              {c.oficial && <span className="tag">Oficial</span>}
              <div className="spacer" />
              {!c.oficial && <button className="ghost mini" onClick={() => apagar(c.id)}>Remover</button>}
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
