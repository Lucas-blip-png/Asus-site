import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { inscrever } from '../ws.js'

// Overlay transparente para OBS (Fase 9): mostra a rolagem mais recente em tempo real.
export default function Overlay() {
  const { campanhaId } = useParams()
  const [ultima, setUltima] = useState(null)

  useEffect(
    () =>
      inscrever(`/topic/campanhas/${campanhaId}/rolagens`, (r) => {
        if (r && r.total != null) setUltima(r)
      }),
    [campanhaId],
  )

  return (
    <div className="overlay-root">
      {ultima ? (
        <div className="overlay-roll pop" key={ultima.id}>
          <div className="muted">{ultima.rotulo || ultima.expressao}</div>
          <div
            className={`overlay-total ${ultima.critico ? 'crit' : ultima.falhaCritica ? 'fumble' : ''}`}
          >
            {ultima.total}
          </div>
          <div className="muted">
            {ultima.detalhe}
            {ultima.critico ? ' · CRÍTICO!' : ''}
            {ultima.falhaCritica ? ' · FALHA!' : ''}
          </div>
        </div>
      ) : (
        <div className="overlay-roll">
          <div className="muted">Aguardando rolagens…</div>
        </div>
      )}
    </div>
  )
}
