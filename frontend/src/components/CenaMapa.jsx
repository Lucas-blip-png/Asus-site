import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { inscrever } from '../ws.js'

const MAPA_W = 16
const MAPA_H = 10
const CORES = ['#e0454e', '#5566e0', '#3aa76d', '#e0a640', '#a45de0', '#e05fa0', '#57c4d4', '#c8cdd6']

const parseFog = (c) => { try { return new Set(JSON.parse(c?.fogJson || '[]')) } catch { return new Set() } }
const parseTokens = (c) => { try { const t = JSON.parse(c?.tokensJson || '[]'); return Array.isArray(t) ? t : [] } catch { return [] } }

/**
 * Mapa 2D de cena/seção. O mestre monta (fundo, névoa, tokens) e ativa a cena;
 * os jogadores enxergam a cena ATIVA da campanha em tempo real.
 */
export default function CenaMapa({ campanhaId, orgId, ehMestre }) {
  const [cenas, setCenas] = useState([])
  const [selId, setSelId] = useState(null)      // cena que o mestre está editando
  const [modo, setModo] = useState('mover')      // 'mover' | 'revelar'
  const [selToken, setSelToken] = useState(null) // token selecionado para mover
  const [novoToken, setNovoToken] = useState({ nome: '', cor: CORES[0] })
  const [erro, setErro] = useState(null)

  const carregar = useCallback(() => api(`/api/campanhas/${campanhaId}/cenas`).then(setCenas).catch(() => {}), [campanhaId])
  useEffect(() => { carregar() }, [carregar])
  // Tempo real: quando o mestre mexe, os jogadores recarregam.
  useEffect(() => inscrever(`/topic/campanhas/${campanhaId}/cenas`, () => carregar()), [campanhaId, carregar])

  // Mestre edita a selecionada (ou a 1ª); jogador vê a ativa.
  const cena = ehMestre
    ? (cenas.find((c) => c.id === selId) || cenas.find((c) => c.ativa) || cenas[0] || null)
    : (cenas.find((c) => c.ativa) || null)

  const fogAtivo = cena?.fogJson != null
  const reveladas = parseFog(cena)
  const tokens = parseTokens(cena)

  async function patch(body) {
    if (!cena) return
    try {
      const atual = await api(`/api/cenas/${cena.id}`, { method: 'PUT', body })
      setCenas((cs) => cs.map((c) => (c.id === atual.id ? atual : c)))
    } catch (e) { setErro(e.message) }
  }
  async function criarCena() {
    try {
      const c = await api(`/api/campanhas/${campanhaId}/cenas`, { method: 'POST', body: { nome: 'Nova cena' } })
      setCenas((cs) => [c, ...cs]); setSelId(c.id)
    } catch (e) { setErro(e.message) }
  }
  async function ativar(cenaId) {
    try { await api(`/api/cenas/${cenaId}/ativar`, { method: 'POST' }); carregar() } catch (e) { setErro(e.message) }
  }
  async function apagar(cenaId) {
    try { await api(`/api/cenas/${cenaId}`, { method: 'DELETE' }); setCenas((cs) => cs.filter((c) => c.id !== cenaId)); if (selId === cenaId) setSelId(null) }
    catch (e) { setErro(e.message) }
  }
  async function uploadFundo(file) {
    if (!file || !cena) return
    setErro(null)
    try {
      const form = new FormData(); form.append('file', file); form.append('tipo', 'OUTRO')
      const asset = await api(`/api/organizacoes/${orgId}/assets`, { method: 'POST', body: form })
      patch({ mapaAssetId: asset.id })
    } catch (e) { setErro(e.message) }
  }
  const toggleNevoa = () => { patch({ fogJson: fogAtivo ? null : '[]' }); setModo(fogAtivo ? 'mover' : 'revelar') }
  function revelar(idx) {
    const s = new Set(reveladas); s.has(idx) ? s.delete(idx) : s.add(idx)
    patch({ fogJson: JSON.stringify([...s]) })
  }
  const salvarTokens = (arr) => patch({ tokensJson: JSON.stringify(arr) })
  function addToken() {
    const nome = novoToken.nome.trim() || 'Token'
    const id = Date.now() + Math.floor(Math.random() * 1000)
    // acha a 1ª célula livre
    let x = 0, y = 0
    for (let i = 0; i < MAPA_W * MAPA_H; i++) { const cx = i % MAPA_W, cy = Math.floor(i / MAPA_W); if (!tokens.some((t) => t.x === cx && t.y === cy)) { x = cx; y = cy; break } }
    salvarTokens([...tokens, { id, nome, cor: novoToken.cor, x, y }])
    setNovoToken({ nome: '', cor: novoToken.cor })
  }
  const removerToken = (id) => salvarTokens(tokens.filter((t) => t.id !== id))

  function clicarCelula(x, y, ocupante) {
    if (!ehMestre) return
    if (fogAtivo && modo === 'revelar') { revelar(y * MAPA_W + x); return }
    if (ocupante) { setSelToken(selToken === ocupante.id ? null : ocupante.id); return }
    if (selToken != null) { salvarTokens(tokens.map((t) => (t.id === selToken ? { ...t, x, y } : t))); setSelToken(null) }
  }

  if (!ehMestre && !cena) {
    return <div className="muted" style={{ padding: 16 }}>O mestre ainda não abriu nenhuma cena. 🗺️</div>
  }

  return (
    <div>
      {erro && <p className="error">{erro}</p>}

      {ehMestre && (
        <div className="row" style={{ gap: 6, flexWrap: 'wrap', alignItems: 'center', marginBottom: 10 }}>
          <select value={cena?.id || ''} onChange={(e) => setSelId(Number(e.target.value))} style={{ maxWidth: 220 }}>
            {!cenas.length && <option value="">— nenhuma cena —</option>}
            {cenas.map((c) => <option key={c.id} value={c.id}>{c.nome}{c.ativa ? ' • ativa' : ''}</option>)}
          </select>
          <button className="mini" onClick={criarCena}>+ Nova cena</button>
          {cena && (
            <>
              <input value={cena.nome} onChange={(e) => setCenas((cs) => cs.map((c) => c.id === cena.id ? { ...c, nome: e.target.value } : c))}
                onBlur={(e) => patch({ nome: e.target.value })} style={{ maxWidth: 160 }} />
              <button className={`ghost mini${cena.ativa ? ' ativo' : ''}`} onClick={() => ativar(cena.id)}
                title="Exibir esta cena para os jogadores">{cena.ativa ? '📡 Ativa' : 'Ativar p/ jogadores'}</button>
              <button className="ghost mini" title="Apagar cena" onClick={() => apagar(cena.id)}>🗑</button>
            </>
          )}
        </div>
      )}

      {cena && ehMestre && (
        <div className="row" style={{ gap: 6, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
          <label className="ghost mini" style={{ cursor: 'pointer' }} title="Imagem de fundo">
            🖼 Fundo
            <input type="file" accept="image/*" style={{ display: 'none' }} onChange={(e) => uploadFundo(e.target.files[0])} />
          </label>
          {cena.mapaAssetId && <button className="ghost mini" onClick={() => patch({ mapaAssetId: null })}>🖼✕</button>}
          <button className={`ghost mini${fogAtivo ? ' ativo' : ''}`} onClick={toggleNevoa} title="Névoa de guerra">🌫 Névoa</button>
          {fogAtivo && (
            <>
              <button className={`ghost mini${modo === 'mover' ? ' ativo' : ''}`} onClick={() => setModo('mover')}>Mover</button>
              <button className={`ghost mini${modo === 'revelar' ? ' ativo' : ''}`} onClick={() => setModo('revelar')}>Revelar</button>
            </>
          )}
          <div className="spacer" />
          <input placeholder="Token" value={novoToken.nome} onChange={(e) => setNovoToken((s) => ({ ...s, nome: e.target.value }))}
            style={{ maxWidth: 110 }} onKeyDown={(e) => { if (e.key === 'Enter') addToken() }} />
          <div className="row" style={{ gap: 3 }}>
            {CORES.map((c) => (
              <button key={c} onClick={() => setNovoToken((s) => ({ ...s, cor: c }))} title="cor"
                style={{ width: 16, height: 16, borderRadius: '50%', background: c, border: novoToken.cor === c ? '2px solid #fff' : '1px solid #0006', cursor: 'pointer' }} />
            ))}
          </div>
          <button className="mini" onClick={addToken}>+ Token</button>
        </div>
      )}

      {ehMestre && (
        <p className="muted" style={{ fontSize: '.74rem', margin: '0 0 6px' }}>
          {fogAtivo && modo === 'revelar' ? 'Modo névoa: clique nas células para revelar/cobrir.'
            : 'Clique num token e depois numa célula para mover.'}
          {selToken != null && <b> Selecionado: {tokens.find((t) => t.id === selToken)?.nome}</b>}
        </p>
      )}

      {!cena ? (
        <div className="muted" style={{ padding: 16 }}>Crie uma cena e ative-a para os jogadores. 🗺️</div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <div style={{
            display: 'grid', gridTemplateColumns: `repeat(${MAPA_W}, 40px)`, gap: 2, width: 'max-content', padding: 4, borderRadius: 8,
            backgroundImage: cena.mapaAssetId ? `url(/api/assets/${cena.mapaAssetId}/conteudo)` : undefined,
            backgroundSize: 'cover', backgroundPosition: 'center',
          }}>
            {Array.from({ length: MAPA_W * MAPA_H }, (_, i) => {
              const x = i % MAPA_W, y = Math.floor(i / MAPA_W)
              const ocupante = tokens.find((t) => t.x === x && t.y === y)
              const coberta = fogAtivo && !reveladas.has(i)
              const fundo = coberta ? (ehMestre ? 'rgba(5,5,10,.55)' : 'rgba(8,8,14,.98)')
                : (cena.mapaAssetId ? 'rgba(255,255,255,.02)' : 'rgba(255,255,255,.04)')
              return (
                <div key={i} onClick={() => clicarCelula(x, y, ocupante)} style={{
                  width: 40, height: 40, borderRadius: 6, background: fundo, border: '1px solid rgba(255,255,255,.08)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: ehMestre ? 'pointer' : 'default',
                  outline: ocupante && selToken === ocupante.id ? '2px solid var(--gold, #e0b64a)' : 'none',
                }}>
                  {ocupante && (!coberta || ehMestre) && (
                    <div title={ocupante.nome} style={{
                      width: 32, height: 32, borderRadius: '50%', background: ocupante.cor, opacity: coberta ? 0.45 : 1,
                      display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 13,
                      color: '#fff', textShadow: '0 1px 2px #000', border: '2px solid rgba(255,255,255,.7)',
                    }}>{(ocupante.nome || '?').charAt(0).toUpperCase()}</div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {ehMestre && cena && tokens.length > 0 && (
        <div className="row" style={{ gap: 6, flexWrap: 'wrap', marginTop: 8 }}>
          {tokens.map((t) => (
            <span key={t.id} className="tag" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <span style={{ width: 10, height: 10, borderRadius: '50%', background: t.cor, display: 'inline-block' }} />
              {t.nome}
              <button className="ghost mini" style={{ padding: '0 4px' }} onClick={() => removerToken(t.id)}>✕</button>
            </span>
          ))}
        </div>
      )}
    </div>
  )
}
