import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'

const TIPOS = ['FICHA', 'CAMPANHA', 'ITEM', 'CRIATURA', 'MAPA', 'OUTRO']
const VAZIO = { titulo: '', descricao: '', tipo: 'FICHA', preco: '', moeda: 'BRL', publicado: true }

function ItemRow({ i, onBuy }) {
  const [open, setOpen] = useState(false)
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{i.titulo}</b>
        {i.tipo && <span className="sub">{i.tipo}</span>}
        <div className="spacer" />
        <span className={`tag ${i.gratuito ? '' : 'gold'}`}>
          {i.gratuito ? 'Grátis' : `${i.moeda || ''} ${i.preco}`}
        </span>
        <button className="mini" onClick={(e) => { e.stopPropagation(); onBuy(i.id) }}>
          {i.gratuito ? 'Obter' : 'Comprar'}
        </button>
      </div>
      {open && (
        <div className="cris-body">
          <p className="muted" style={{ fontSize: '.84rem', margin: 0 }}>{i.descricao || 'Sem descrição.'}</p>
        </div>
      )}
    </div>
  )
}

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
      <div className="cris-list">
        {filtrados.map((i) => <ItemRow key={i.id} i={i} onBuy={comprar} />)}
      </div>
    </>
  )
}
