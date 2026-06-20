import { useEffect, useState } from 'react'
import { api, obterOrgId } from '../api.js'
import { useAuth } from '../auth.jsx'

const TIPOS = ['FICHA', 'CAMPANHA', 'NPC', 'ITEM', 'OUTRO']
const VAZIO = { tipo: 'FICHA', nome: '', descricao: '', jsonConteudo: '', publico: false }

function CardTemplate({ t, onDelete }) {
  return (
    <div className="card">
      <div className="row">
        <b style={{ fontSize: '1.02rem' }}>{t.nome}</b>
        <div className="spacer" />
        <span className="tag">{t.tipo}</span>
        {t.oficial && <span className="tag gold">Oficial</span>}
        {t.publico && <span className="tag">Público</span>}
      </div>
      {t.descricao && <p className="muted" style={{ margin: '6px 0', fontSize: '.84rem' }}>{t.descricao}</p>}
      {onDelete && !t.oficial && (
        <div className="row" style={{ marginTop: 6 }}>
          <div className="spacer" />
          <button className="ghost mini" onClick={() => onDelete(t.id)}>Remover</button>
        </div>
      )}
    </div>
  )
}

export default function Templates() {
  const { user } = useAuth()
  const [orgId, setOrgId] = useState(null)
  const [meus, setMeus] = useState([])
  const [publicos, setPublicos] = useState([])
  const [nova, setNova] = useState(VAZIO)
  const [abrir, setAbrir] = useState(false)
  const [erro, setErro] = useState(null)

  const carregar = async (oid) => {
    setMeus(await api(`/api/organizacoes/${oid}/templates`))
    setPublicos(await api('/api/templates/publicos'))
  }
  useEffect(() => {
    (async () => { const oid = await obterOrgId(); setOrgId(oid); await carregar(oid) })().catch((e) => setErro(e.message))
  }, [])

  async function criar() {
    if (!nova.nome.trim()) return
    setErro(null)
    try {
      await api(`/api/organizacoes/${orgId}/templates`, { method: 'POST', body: { ...nova, autorUsuarioId: user?.id } })
      setNova(VAZIO); setAbrir(false); carregar(orgId)
    } catch (e) { setErro(e.message) }
  }
  async function apagar(id) {
    try { await api(`/api/templates/${id}`, { method: 'DELETE' }); carregar(orgId) } catch (e) { setErro(e.message) }
  }

  return (
    <>
      <div className="page-head">
        <h1>Templates</h1>
        <span className="count-badge"><b>{meus.length}</b></span>
        <div className="spacer" />
        <button onClick={() => setAbrir((v) => !v)}>{abrir ? 'Fechar' : '+ Novo template'}</button>
      </div>
      {erro && <p className="error">{erro}</p>}

      {abrir && (
        <div className="card add-form" style={{ marginBottom: 16 }}>
          <input placeholder="Nome" value={nova.nome}
            onChange={(e) => setNova((s) => ({ ...s, nome: e.target.value }))} />
          <select value={nova.tipo} onChange={(e) => setNova((s) => ({ ...s, tipo: e.target.value }))}>
            {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <label className="row" style={{ gap: 6, width: 'auto' }}>
            <input type="checkbox" style={{ width: 'auto' }} checked={nova.publico}
              onChange={(e) => setNova((s) => ({ ...s, publico: e.target.checked }))} /> público
          </label>
          <input placeholder="Descrição" style={{ flexBasis: '100%' }} value={nova.descricao}
            onChange={(e) => setNova((s) => ({ ...s, descricao: e.target.value }))} />
          <textarea placeholder="Conteúdo (JSON)" style={{ flexBasis: '100%', minHeight: 70 }} value={nova.jsonConteudo}
            onChange={(e) => setNova((s) => ({ ...s, jsonConteudo: e.target.value }))} />
          <button className="mini" onClick={criar}>Salvar</button>
        </div>
      )}

      <h2>Da organização</h2>
      {!meus.length && <div className="card muted">Nenhum template seu ainda.</div>}
      <div className="grid">
        {meus.map((t) => <CardTemplate key={t.id} t={t} onDelete={apagar} />)}
      </div>

      <h2 style={{ marginTop: 18 }}>Públicos</h2>
      {!publicos.length && <div className="card muted">Nenhum template público disponível.</div>}
      <div className="grid">
        {publicos.map((t) => <CardTemplate key={t.id} t={t} />)}
      </div>
    </>
  )
}
