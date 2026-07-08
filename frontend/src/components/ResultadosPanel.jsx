import { useState } from 'react'
import { dataHora } from '../format.js'

/**
 * Painel flutuante de "Resultados" (rolagens + chat de texto), aberto por um botão flutuante.
 * - rolagens: lista de rolagens (mais recentes primeiro)
 * - mensagens: mensagens de texto da campanha (opcional; mais recentes primeiro)
 * - onRolar(expressao, rotulo, privada): dispara uma rolagem
 * - onMensagem(texto): envia uma mensagem de texto (opcional; habilita o campo de chat)
 * - ehMestre: mostra a opção pública/privada e os botões de revelar
 * - onRevelar(id): revela uma rolagem oculta (só mestre)
 */
export default function ResultadosPanel({ rolagens = [], mensagens = [], onRolar, onMensagem, ehMestre = false, onRevelar }) {
  const [aberto, setAberto] = useState(false)
  const [expr, setExpr] = useState('1d20')
  const [rotulo, setRotulo] = useState('')
  const [privada, setPrivada] = useState(false)
  const [texto, setTexto] = useState('')
  const [erro, setErro] = useState(null)
  // Quantos itens já foram "vistos" (abrir o painel marca tudo como visto).
  const [vistas, setVistas] = useState(0)

  const total = rolagens.length + mensagens.length
  const naoVistas = Math.max(0, total - vistas)
  function alternar() {
    setVistas(total)
    setAberto((o) => !o)
  }

  // Mistura rolagens e mensagens numa linha do tempo (mais recente primeiro).
  const itens = [
    ...rolagens.map((r) => ({ tipo: 'rolagem', chave: 'r' + r.id, ...r })),
    ...mensagens.map((m) => ({ tipo: 'mensagem', chave: 'm' + m.id, ...m })),
  ].sort((a, b) => new Date(b.criadoEm) - new Date(a.criadoEm))

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

  async function enviarTexto(e) {
    e.preventDefault()
    if (!texto.trim() || !onMensagem) return
    setErro(null)
    try {
      await onMensagem(texto.trim())
      setTexto('')
    } catch (ex) {
      setErro(ex.message)
    }
  }

  return (
    <>
      <button className="chat-fab" title="Resultados e chat" onClick={alternar}>
        💬
        {!aberto && naoVistas > 0 && <span className="chat-badge">{naoVistas > 99 ? '99+' : naoVistas}</span>}
      </button>

      {aberto && (
        <div className="resultados-panel">
          <div className="rp-head">
            <b>Resultados</b>
            <div className="spacer" />
            <button className="ghost mini" onClick={() => { setVistas(total); setAberto(false) }}>✕</button>
          </div>

          <form className="rp-roll" onSubmit={enviar}>
            <input value={expr} onChange={(e) => setExpr(e.target.value)} placeholder="1d20+5" style={{ width: 90 }} />
            <input value={rotulo} onChange={(e) => setRotulo(e.target.value)} placeholder="Rótulo (ex.: Furtividade)" />
            <button className="mini" type="submit">🎲</button>
          </form>
          {onMensagem && (
            <form className="rp-roll" onSubmit={enviarTexto}>
              <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Mensagem pro grupo…" style={{ flex: 1 }} />
              <button className="mini" type="submit">💬</button>
            </form>
          )}
          {ehMestre && (
            <div className="rp-vis">
              <button type="button" className={`vis ${!privada ? 'on' : ''}`} onClick={() => setPrivada(false)}>Público</button>
              <button type="button" className={`vis ${privada ? 'on priv' : ''}`} onClick={() => setPrivada(true)}>Privado</button>
            </div>
          )}
          {erro && <p className="error" style={{ margin: '6px 12px' }}>{erro}</p>}

          <div className="rp-lista">
            {itens.map((r) => {
              if (r.tipo === 'mensagem') {
                return (
                  <div key={r.chave} className="res-item">
                    <div className="hex" style={{ color: 'var(--muted)' }}>💬</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="rt"><b style={{ color: 'var(--gold, #e0b64a)' }}>{r.autor || 'Jogador'}</b></div>
                      <div style={{ fontSize: '.82rem', wordBreak: 'break-word' }}>{r.texto}</div>
                      <div className="muted" style={{ fontSize: '.68rem' }}>{dataHora(r.criadoEm)}</div>
                    </div>
                  </div>
                )
              }
              const oculto = r.oculta && r.total == null
              const cor = r.critico ? 'var(--crit)' : r.falhaCritica ? 'var(--fumble)' : 'var(--text)'
              return (
                <div key={r.chave} className={`res-item${r.oculta ? ' privada' : ''}`}>
                  <div className="hex" style={{ color: oculto ? 'var(--muted)' : cor }}>
                    {oculto ? '🎲' : (r.total ?? '—')}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="rt">
                      {r.personagemNome && <b style={{ color: 'var(--gold, #e0b64a)' }}>{r.personagemNome} · </b>}
                      {r.rotulo || r.expressao}{r.oculta ? ' 🔒' : ''}
                    </div>
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
            {!itens.length && <div className="muted" style={{ padding: 12 }}>Nenhuma rolagem ainda.</div>}
          </div>
        </div>
      )}
    </>
  )
}
