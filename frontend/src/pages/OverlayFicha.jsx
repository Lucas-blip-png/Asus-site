import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'

// Barra de recurso (VIDA/MANA/ENERGIA) para o overlay OBS.
function Barra({ rot, cor, atual = 0, max = 0 }) {
  const pct = max > 0 ? Math.min(100, Math.round((atual / max) * 100)) : 0
  return (
    <div style={{ width: 250, marginTop: 9 }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between', fontSize: 12, fontWeight: 700,
        letterSpacing: 1.5, color: '#fff', textShadow: '0 1px 3px rgba(0,0,0,.85)', marginBottom: 3,
      }}>
        <span>{rot}</span><span>{atual}/{max}</span>
      </div>
      <div style={{
        height: 22, borderRadius: 12, background: 'rgba(0,0,0,.5)',
        border: '2px solid rgba(255,255,255,.85)', overflow: 'hidden', boxShadow: '0 3px 12px rgba(0,0,0,.55)',
      }}>
        <div style={{ width: pct + '%', height: '100%', background: cor, transition: 'width .3s ease' }} />
      </div>
    </div>
  )
}

// Overlay OBS por personagem (retrato/portrait): mostra o retrato, as barras de
// VIDA/MANA/ENERGIA e a última rolagem que ELE fez, tudo em tempo real.
// Página pública (sem login), pensada para ser Browser Source no OBS.
export default function OverlayFicha() {
  const { personagemId } = useParams()
  const [info, setInfo] = useState(null)
  const [status, setStatus] = useState(null)
  const [ultima, setUltima] = useState(null)
  const [fotoErro, setFotoErro] = useState(false)

  useEffect(() => {
    setFotoErro(false)
    api(`/api/personagens/${personagemId}/overlay`, { auth: false })
      .then((d) => { setInfo(d); setStatus(d.status || null) })
      .catch(() => {})
  }, [personagemId])

  // Rolagens do personagem em tempo real.
  useEffect(
    () =>
      inscrever(`/topic/personagens/${personagemId}/rolagens`, (r) => {
        if (r && r.total != null) setUltima(r)
      }),
    [personagemId],
  )
  // Status (PV/PM/PE) em tempo real: se o mestre ou o jogador muda, o overlay atualiza.
  useEffect(
    () => inscrever(`/topic/personagens/${personagemId}/status`, (s) => s && setStatus(s)),
    [personagemId],
  )

  const critClass = ultima?.critico ? 'crit' : ultima?.falhaCritica ? 'fumble' : ''
  const temFoto = info?.avatarAssetId && !fotoErro
  const circulo = {
    width: 132, height: 132, borderRadius: '50%', overflow: 'hidden',
    border: '3px solid rgba(255,255,255,.85)', boxShadow: '0 6px 26px rgba(0,0,0,.55)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: 52, fontWeight: 700, color: '#fff', background: '#3a3a55',
  }

  return (
    <div className="overlay-root">
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
        <div style={circulo}>
          {temFoto ? (
            <img src={`/api/assets/${info.avatarAssetId}/conteudo`} alt=""
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              onError={() => setFotoErro(true)} />
          ) : (info?.nome || '?').charAt(0).toUpperCase()}
        </div>
        {info?.nome && (
          <div style={{ fontWeight: 700, fontSize: 18, color: '#fff', textShadow: '0 1px 4px rgba(0,0,0,.85)' }}>
            {info.nome}
          </div>
        )}

        {status && (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'stretch' }}>
            <Barra rot="VIDA" cor="linear-gradient(90deg,#e0454e,#ff6b73)" atual={status.pvAtual} max={status.pvMax} />
            <Barra rot="MANA" cor="linear-gradient(90deg,#5566e0,#8a97ff)" atual={status.pmAtual} max={status.pmMax} />
            <Barra rot="ENERGIA" cor="linear-gradient(90deg,#e0a640,#ffce6b)" atual={status.peAtual} max={status.peMax} />
          </div>
        )}

        {ultima ? (
          <div className="overlay-roll pop" key={ultima.id} style={{ marginTop: 6 }}>
            <div className="muted">{ultima.rotulo || ultima.expressao}</div>
            <div className={`overlay-total ${critClass}`}>{ultima.total}</div>
            <div className="muted">
              {ultima.detalhe}
              {ultima.critico ? ' · CRÍTICO!' : ''}
              {ultima.falhaCritica ? ' · FALHA!' : ''}
            </div>
          </div>
        ) : (
          <div className="overlay-roll" style={{ marginTop: 6 }}>
            <div className="muted" style={{ fontSize: '.9rem' }}>Aguardando rolagens…</div>
          </div>
        )}
      </div>
    </div>
  )
}
