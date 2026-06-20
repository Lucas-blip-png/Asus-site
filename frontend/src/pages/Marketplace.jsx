import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'

const TIPOS = ['FICHA', 'CAMPANHA', 'ITEM', 'CRIATURA', 'MAPA', 'OUTRO']
const VAZIO = { titulo: '', descricao: '', tipo: 'FICHA', preco: '', moeda: 'BRL', publicado: true }

export default function Marketplace() {
  const { user } = useAuth()
  const [itens, setItens] = useState([])
  const [busca, setBusca] = useState('')
  const [nova, setNova] = useState(VAZIO)
  const [abrir, setAbrir] = useState(false)
  const [msg, setMsg] = useState(null)
  const [erro, setErro] = useState(null)

  const carregar = () => api('/api/marketplace').then(setItens).catch((e) => setErro(e.message))
  useEffect(() => { carregar() }, [])

  async function criar() {
    if (!nova.titulo.trim()) return
    setErro(null)
    try {
      await api('/api/marketplace/items', {
        method: 'POST',
        body: { ...nova, preco: nova.preco === '' ? null : Number(nova.preco), autorUsuarioId: user?.id },
      })
      setNova(VAZIO); setAbrir(false); carregar()
    } catch (e) { setErro(e.message) }
  }
  async function comprar(id) {
    setErro(null); setMsg(null)
    try {
      const c = await api(`/api/marketplace/items/${id}/comprar`, { method: 'POST', body: { usuarioId: user?.id } })
      setMsg(`Compra concluída (#${c.id}).`)
    } catch (e) { setErro(e.message) }
  }

  const filtrados = itens.filter((i) =>
    (i.titulo + ' ' + (i.tipo || '') + ' ' + (i.descricao || '')).toLowerCase().includes(busca.toLowerCase()))

  return (
    <>
      <div className="page-head">
        <h1>Marketplace</h1>
        <span className="count-badge"><b>{itens.length}</b></span>
        <div className="spacer" />
        <button onClick={() => setAbrir((v) => !v)}>{abrir ? 'Fechar' : '+ Publicar item'}</button>
      </div>
      {erro && <p className="error">{erro}</p>}
      {msg && <p className="ok">{msg}</p>}

      <div className="search-wrap">
        <span className="ic">🔍</span>
        <input placeholder="Buscar no marketplace" value={busca} onChange={(e) => setBusca(e.target.value)} />
      </div>

      {abrir && (
        <div className="card add-form" style={{ marginBottom: 16 }}>
          <input placeholder="Título" value={nova.titulo}
            onChange={(e) => setNova((s) => ({ ...s, titulo: e.target.value }))} />
          <select value={nova.tipo} onChange={(e) => setNova((s) => ({ ...s, tipo: e.target.value }))}>
            {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <input type="number" placeholder="Preço (vazio = grátis)" style={{ maxWidth: 170 }} value={nova.preco}
            onChange={(e) => setNova((s) => ({ ...s, preco: e.target.value }))} />
          <input placeholder="Moeda" style={{ maxWidth: 90 }} value={nova.moeda}
            onChange={(e) => setNova((s) => ({ ...s, moeda: e.target.value }))} />
          <input placeholder="Descrição" style={{ flexBasis: '100%' }} value={nova.descricao}
            onChange={(e) => setNova((s) => ({ ...s, descricao: e.target.value }))} />
          <button className="mini" onClick={criar}>Publicar</button>
        </div>
      )}

      {!filtrados.length && <div className="card muted">Nenhum item publicado ainda.</div>}
      <div className="grid">
        {filtrados.map((i) => (
          <div key={i.id} className="card">
            <div className="row">
              <b style={{ fontSize: '1.02rem' }}>{i.titulo}</b>
              <div className="spacer" />
              {i.tipo && <span className="tag">{i.tipo}</span>}
            </div>
            {i.descricao && <p className="muted" style={{ margin: '6px 0', fontSize: '.84rem' }}>{i.descricao}</p>}
            <div className="row" style={{ marginTop: 8 }}>
              <span className={`tag ${i.gratuito ? '' : 'gold'}`}>
                {i.gratuito ? 'Grátis' : `${i.moeda || ''} ${i.preco}`}
              </span>
              <div className="spacer" />
              <button className="mini" onClick={() => comprar(i.id)}>{i.gratuito ? 'Obter' : 'Comprar'}</button>
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
