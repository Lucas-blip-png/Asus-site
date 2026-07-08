import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'

// Barra fininha de recurso para os cards da mesa.
function Mini({ cor, atual = 0, max = 0 }) {
  const pct = max > 0 ? Math.min(100, Math.round((atual / max) * 100)) : 0
  return (
    <div style={{ height: 7, borderRadius: 4, background: 'rgba(0,0,0,.55)', border: '1px solid rgba(255,255,255,.35)', overflow: 'hidden', marginTop: 3 }}>
      <div style={{ width: pct + '%', height: '100%', background: cor, transition: 'width .3s ease' }} />
    </div>
  )
}

/**
 * Overlay OBS da MESA inteira: retratos + barras de todo o grupo, ordem de
 * iniciativa do combate ativo e a última rolagem. Página pública.
 *
 * Personalização por URL: ?barras=0 ?dado=0 ?nome=0 ?iniciativa=0
 * ?escala=1.25 (tamanho) ?cor=e0b64a (cor de destaque, hex sem #).
 */
export default function OverlayMesa() {
  const { campanhaId } = useParams()
  const [params] = useSearchParams()
  const [dados, setDados] = useState(null)
  const [ultima, setUltima] = useState(null)
  const timer = useRef(null)

  const mostraBarras = params.get('barras') !== '0'
  const mostraDado = params.get('dado') !== '0'
  const mostraNome = params.get('nome') !== '0'
  const mostraIniciativa = params.get('iniciativa') !== '0'
  const escala = Math.max(0.5, Math.min(2.5, Number(params.get('escala')) || 1))
  const cor = /^[0-9a-fA-F]{6}$/.test(params.get('cor') || '') ? '#' + params.get('cor') : '#e0b64a'

  const carregar = () => api(`/api/campanhas/${campanhaId}/overlay`, { auth: false }).then(setDados).catch(() => {})

  useEffect(() => {
    carregar()
    // Iniciativa/rodada não têm websocket — atualiza a cada 8s.
    timer.current = setInterval(carregar, 8000)
    return () => clearInterval(timer.current)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [campanhaId])

  // Barras em tempo real: um tópico de status por personagem.
  const ids = (dados?.personagens || []).map((p) => p.id).join(',')
  useEffect(() => {
    const lista = ids ? ids.split(',') : []
    const subs = lista.map((pid) =>
      inscrever(`/topic/personagens/${pid}/status`, (s) =>
        setDados((prev) => (prev ? {
          ...prev,
          personagens: prev.personagens.map((x) =>
            String(x.id) === String(pid) ? { ...x, status: { ...x.status, ...s } } : x),
        } : prev))),
    )
    return () => subs.forEach((u) => u && u())
  }, [ids])

  // Última rolagem da campanha.
  useEffect(
    () => inscrever(`/topic/campanhas/${campanhaId}/rolagens`, (r) => {
      if (r && r.total != null) setUltima(r)
    }),
    [campanhaId],
  )

  const combate = dados?.combate

  return (
    <div className="overlay-root" style={{ alignItems: 'flex-end' }}>
      <div style={{ transform: `scale(${escala})`, transformOrigin: 'bottom left', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {/* Iniciativa do combate ativo */}
        {mostraIniciativa && combate && (combate.participantes || []).length > 0 && (
          <div style={{
            display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap',
            background: 'rgba(12,10,16,.92)', border: '1px solid rgba(255,255,255,.14)',
            borderLeft: `3px solid ${cor}`, borderRadius: 12, padding: '8px 12px',
          }}>
            <span style={{ fontSize: '.66rem', fontWeight: 800, letterSpacing: 1.4, color: cor }}>
              ⚔ RODADA {combate.rodada}
            </span>
            {combate.participantes.map((pp, i) => (
              <span key={i} title={`${pp.nome} (${pp.iniciativa})`} style={{
                display: 'flex', alignItems: 'center', gap: 5, padding: '2px 8px', borderRadius: 8,
                fontSize: '.72rem', fontWeight: 700, color: '#fff',
                background: i === combate.turnoAtual ? cor : 'rgba(255,255,255,.08)',
                ...(i === combate.turnoAtual ? { color: '#1a1408' } : {}),
              }}>
                {pp.inimigo ? '👹' : '🛡'} {pp.nome} <small style={{ opacity: .75 }}>{pp.iniciativa}</small>
              </span>
            ))}
          </div>
        )}

        {/* Cards do grupo */}
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {(dados?.personagens || []).map((p) => {
            const s = p.status || {}
            return (
              <div key={p.id} style={{
                display: 'flex', gap: 9, alignItems: 'center', width: 205,
                background: 'rgba(12,10,16,.92)', border: '1px solid rgba(255,255,255,.14)',
                borderLeft: `3px solid ${cor}`, borderRadius: 12, padding: '9px 11px',
              }}>
                <div style={{
                  width: 44, height: 44, borderRadius: '50%', flexShrink: 0, overflow: 'hidden',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontWeight: 800, fontSize: 19, color: '#fff', background: '#3a3a55',
                  backgroundImage: p.avatarAssetId ? `url(/api/assets/${p.avatarAssetId}/conteudo)` : undefined,
                  backgroundSize: 'cover', backgroundPosition: 'center',
                }}>
                  {!p.avatarAssetId && (p.nome || '?').charAt(0).toUpperCase()}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  {mostraNome && (
                    <div style={{
                      fontWeight: 800, fontSize: 13, color: '#fff', whiteSpace: 'nowrap',
                      overflow: 'hidden', textOverflow: 'ellipsis', textShadow: '0 1px 3px #000',
                    }}>{p.nome}</div>
                  )}
                  {mostraBarras && (
                    <>
                      <Mini cor="linear-gradient(90deg,#e0454e,#ff6b73)" atual={s.pvAtual} max={s.pvMax} />
                      <Mini cor="linear-gradient(90deg,#5566e0,#8a97ff)" atual={s.pmAtual} max={s.pmMax} />
                      <Mini cor="linear-gradient(90deg,#e0a640,#ffce6b)" atual={s.peAtual} max={s.peMax} />
                    </>
                  )}
                </div>
              </div>
            )
          })}
        </div>

        {/* Última rolagem */}
        {mostraDado && ultima && (
          <div className="pop" key={ultima.id} style={{
            display: 'flex', alignItems: 'center', gap: 12, width: 'max-content',
            background: 'rgba(12,10,16,.92)', border: '1px solid rgba(255,255,255,.14)',
            borderLeft: `3px solid ${cor}`, borderRadius: 12, padding: '8px 14px',
          }}>
            <div className={`overlay-total ${ultima.critico ? 'crit' : ultima.falhaCritica ? 'fumble' : ''}`}
              style={{ fontSize: '2rem', margin: 0 }}>
              {ultima.total ?? '🎲'}
            </div>
            <div style={{ minWidth: 0 }}>
              <div style={{ textTransform: 'uppercase', letterSpacing: 1.2, fontSize: '.66rem', fontWeight: 700, color: cor }}>
                {ultima.personagemNome ? `${ultima.personagemNome} · ` : ''}{ultima.rotulo || ultima.expressao}
              </div>
              <div className="muted" style={{ fontSize: '.68rem' }}>
                {ultima.detalhe}
                {ultima.critico ? ' · CRÍTICO!' : ''}
                {ultima.falhaCritica ? ' · FALHA!' : ''}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
