import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'

// Barra de recurso (VIDA/MANA/ENERGIA), largura total do card.
function BarraV({ rot, cor, atual = 0, max = 0 }) {
  const pct = max > 0 ? Math.min(100, Math.round((atual / max) * 100)) : 0
  return (
    <div style={{ marginBottom: 9 }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between', fontSize: 11, fontWeight: 800,
        letterSpacing: 1.5, color: '#fff', textShadow: '0 1px 3px rgba(0,0,0,.85)', marginBottom: 3,
      }}>
        <span>{rot}</span><span>{atual}/{max}</span>
      </div>
      <div style={{
        height: 18, borderRadius: 10, background: 'rgba(0,0,0,.55)',
        border: '1.5px solid rgba(255,255,255,.7)', overflow: 'hidden',
      }}>
        <div style={{ width: pct + '%', height: '100%', background: cor, transition: 'width .3s ease' }} />
      </div>
    </div>
  )
}

// Overlay OBS por personagem no formato retrato (vertical): retrato grande com o
// nome sobreposto, barras de VIDA/MANA/ENERGIA e a última rolagem — tudo em tempo real.
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

  useEffect(
    () => inscrever(`/topic/personagens/${personagemId}/rolagens`, (r) => {
      if (r && r.total != null) setUltima(r)
    }),
    [personagemId],
  )
  useEffect(
    () => inscrever(`/topic/personagens/${personagemId}/status`, (s) => s && setStatus(s)),
    [personagemId],
  )

  const critClass = ultima?.critico ? 'crit' : ultima?.falhaCritica ? 'fumble' : ''
  const temFoto = info?.avatarAssetId && !fotoErro
  const inicial = (info?.nome || '?').charAt(0).toUpperCase()

  return (
    <div className="overlay-root">
      <div style={{
        width: 300,
        background: 'linear-gradient(180deg, rgba(24,20,30,.94), rgba(12,10,16,.96))',
        borderRadius: 18, border: '1px solid rgba(255,255,255,.14)',
        borderLeft: '3px solid var(--gold, #e0b64a)',
        boxShadow: '0 16px 46px rgba(0,0,0,.6)', overflow: 'hidden', backdropFilter: 'blur(6px)',
      }}>
        {/* Retrato (vertical) com o nome sobreposto */}
        <div style={{ position: 'relative', width: '100%', aspectRatio: '3 / 4', background: '#2a2a3a' }}>
          {temFoto ? (
            <img src={`/api/assets/${info.avatarAssetId}/conteudo`} alt=""
              style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
              onError={() => setFotoErro(true)} />
          ) : (
            <div style={{
              width: '100%', height: '100%', display: 'flex', alignItems: 'center',
              justifyContent: 'center', fontSize: 96, fontWeight: 800, color: '#fff',
            }}>{inicial}</div>
          )}
          <div style={{
            position: 'absolute', left: 0, right: 0, bottom: 0, padding: '44px 16px 12px',
            background: 'linear-gradient(to top, rgba(0,0,0,.92), rgba(0,0,0,.45) 55%, transparent)',
          }}>
            <div style={{ fontWeight: 800, fontSize: 22, color: '#fff', textShadow: '0 2px 6px #000', lineHeight: 1.15 }}>
              {info?.nome || 'Personagem'}
            </div>
          </div>
        </div>

        {/* Barras */}
        {status && (
          <div style={{ padding: '14px 16px 4px' }}>
            <BarraV rot="VIDA" cor="linear-gradient(90deg,#e0454e,#ff6b73)" atual={status.pvAtual} max={status.pvMax} />
            <BarraV rot="MANA" cor="linear-gradient(90deg,#5566e0,#8a97ff)" atual={status.pmAtual} max={status.pmMax} />
            <BarraV rot="ENERGIA" cor="linear-gradient(90deg,#e0a640,#ffce6b)" atual={status.peAtual} max={status.peMax} />
          </div>
        )}

        {/* Última rolagem */}
        <div style={{ padding: '8px 16px 16px', borderTop: '1px solid rgba(255,255,255,.08)', marginTop: 4 }}>
          {ultima ? (
            <div className="pop" key={ultima.id} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div className={`overlay-total ${critClass}`} style={{ fontSize: '2.6rem', margin: 0 }}>{ultima.total}</div>
              <div style={{ minWidth: 0 }}>
                <div style={{ textTransform: 'uppercase', letterSpacing: 1.5, fontSize: '.68rem', fontWeight: 700, color: 'var(--gold-3, #e0b64a)' }}>
                  {ultima.rotulo || ultima.expressao}
                </div>
                <div className="muted" style={{ fontSize: '.7rem' }}>
                  {ultima.detalhe}
                  {ultima.critico ? ' · CRÍTICO!' : ''}
                  {ultima.falhaCritica ? ' · FALHA!' : ''}
                </div>
              </div>
            </div>
          ) : (
            <div className="muted" style={{ fontSize: '.8rem', textAlign: 'center' }}>Aguardando rolagens…</div>
          )}
        </div>
      </div>
    </div>
  )
}
