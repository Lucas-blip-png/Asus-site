import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'

// Overlay OBS por personagem (retrato/portrait): mostra o retrato do personagem
// e a última rolagem que ELE fez, em tempo real. Página pública (sem login),
// pensada para ser usada como Browser Source no OBS.
export default function OverlayFicha() {
  const { personagemId } = useParams()
  const [info, setInfo] = useState(null)
  const [ultima, setUltima] = useState(null)

  useEffect(() => {
    api(`/api/personagens/${personagemId}/overlay`, { auth: false }).then(setInfo).catch(() => {})
  }, [personagemId])

  useEffect(
    () =>
      inscrever(`/topic/personagens/${personagemId}/rolagens`, (r) => {
        if (r && r.total != null) setUltima(r)
      }),
    [personagemId],
  )

  const critClass = ultima?.critico ? 'crit' : ultima?.falhaCritica ? 'fumble' : ''
  const foto = {
    width: 132, height: 132, borderRadius: '50%',
    backgroundSize: 'cover', backgroundPosition: 'center',
    border: '3px solid rgba(255,255,255,.85)', boxShadow: '0 6px 26px rgba(0,0,0,.55)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: 52, fontWeight: 700, color: '#fff',
    background: info?.avatarAssetId ? undefined : '#3a3a55',
    backgroundImage: info?.avatarAssetId ? `url(/api/assets/${info.avatarAssetId}/conteudo)` : undefined,
  }

  return (
    <div className="overlay-root">
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
        <div style={foto}>
          {!info?.avatarAssetId && (info?.nome || '?').charAt(0).toUpperCase()}
        </div>
        {ultima ? (
          <div className="overlay-roll pop" key={ultima.id}>
            <div className="muted">
              {info?.nome ? info.nome + ' · ' : ''}{ultima.rotulo || ultima.expressao}
            </div>
            <div className={`overlay-total ${critClass}`}>{ultima.total}</div>
            <div className="muted">
              {ultima.detalhe}
              {ultima.critico ? ' · CRÍTICO!' : ''}
              {ultima.falhaCritica ? ' · FALHA!' : ''}
            </div>
          </div>
        ) : (
          <div className="overlay-roll">
            <div className="muted">{info?.nome || 'Personagem'}</div>
            <div className="muted" style={{ fontSize: '.9rem' }}>Aguardando rolagens…</div>
          </div>
        )}
      </div>
    </div>
  )
}
