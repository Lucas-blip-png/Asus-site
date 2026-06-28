import { useState } from 'react'
import { dataHora } from '../format.js'

/**
 * Painel flutuante de "Resultados" (chat de dados), aberto por um botão flutuante.
 * - rolagens: lista de rolagens (mais recentes primeiro)
 * - onRolar(expressao, rotulo, privada): dispara uma rolagem
 * - ehMestre: mostra a opção pública/privada e os botões de revelar
 * - onRevelar(id): revela uma rolagem oculta (só mestre)
 */
export default function ResultadosPanel({ rolagens = [], onRolar, ehMestre = false, onRevelar }) {
  const [aberto, setAberto] = useState(false)
  const [expr, setExpr] = useState('1d20')
  const [rotulo, setRotulo] = useState('')
  const [privada, setPrivada] = useState(false)
  const [erro, setErro] = useState(null)

  const naoVistas = rolagens.length

  async function enviar(e) {
    e.preventDefault()
    if (!expr.trim()) return
    setErro(null)
    try {
      await onRolar(expr.trim(), rotulo.trim(), ehMestre ? privada : false)
      setRotulo('')
    } catch (ex) {
      setErro(ex.message)
    }
  }

  return (
    <>
      <button className="chat-fab" title="Resultados de dados" onClick={() => setAberto((o) => !o)}>
        💬
        {!aberto && naoVistas > 0 && <span className="chat-badge">{naoVistas > 99 ? '99+' : naoVistas}</span>}
      </button>

      {aberto && (
        <div className="resultados-panel">
          <div className="rp-head">
            <b>Resultados</b>
            <div className="spacer" />
            <button className="ghost mini" onClick={() => setAberto(false)}>✕</button>
          </div>

          <form className="rp-roll" onSubmit={enviar}>
            <input value={expr} onChange={(e) => setExpr(e.target.value)} placeholder="1d20+5" style={{ width: 90 }} />
            <input value={rotulo} onChange={(e) => setRotulo(e.target.value)} placeholder="Rótulo (ex.: Furtividade)" />
            <button className="mini" type="submit">🎲</button>
          </form>
          {ehMestre && (
            <div className="rp-vis">
              <button type="button" className={`vis ${!privada ? 'on' : ''}`} onClick={() => setPrivada(false)}>Público</button>
              <button type="button" className={`vis ${privada ? 'on priv' : ''}`} onClick={() => setPrivada(true)}>Privado</button>
            </div>
          )}
          {erro && <p className="error" style={{ margin: '6px 12px' }}>{erro}</p>}

          <div className="rp-lista">
            {rolagens.map((r) => {
              const oculto = r.oculta && r.total == null
              const cor = r.critico ? 'var(--crit)' : r.falhaCritica ? 'var(--fumble)' : 'var(--text)'
              return (
                <div key={r.id} className={`res-item${r.oculta ? ' privada' : ''}`}>
                  <div className="hex" style={{ color: oculto ? 'var(--muted)' : cor }}>
                    {oculto ? '🎲' : (r.total ?? '—')}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="rt">{r.rotulo || r.expressao}{r.oculta ? ' 🔒' : ''}</div>
                    <div className="muted" style={{ fontSize: '.72rem' }}>
                      {oculto ? 'rolagem privada' : (r.detalhe || r.expressao)}
                    </div>
                    <div className="muted" style={{ fontSize: '.68rem' }}>{dataHora(r.criadoEm)}</div>
                  </div>
                  {ehMestre && onRevelar && r.oculta && !r.revelada && (
                    <button className="ghost mini" onClick={() => onRevelar(r.id)} title="Revelar para todos">👁</button>
                  )}
                </div>
              )
            })}
            {!rolagens.length && <div className="muted" style={{ padding: 12 }}>Nenhuma rolagem ainda.</div>}
          </div>
        </div>
      )}
    </>
  )
}
