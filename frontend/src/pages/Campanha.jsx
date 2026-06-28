import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'

export default function Campanha() {
  const { id } = useParams()
  const nav = useNavigate()
  const { user } = useAuth()
  const [confirmarApagar, setConfirmarApagar] = useState(false)
  const [campanha, setCampanha] = useState(null)
  const [rolagens, setRolagens] = useState([])
  const [membros, setMembros] = useState([])
  const [personagens, setPersonagens] = useState([])
  const [expr, setExpr] = useState('1d20')
  const [rotulo, setRotulo] = useState('')
  const [oculta, setOculta] = useState(false)
  const [convite, setConvite] = useState(null)
  const [sessoes, setSessoes] = useState([])
  const [sessaoForm, setSessaoForm] = useState({ titulo: '', inicio: '' })
  const [toast, setToast] = useState(null)
  const toastTimer = useRef(null)
  const [erro, setErro] = useState(null)

  function carregar() {
    api(`/api/campanhas/${id}`).then(setCampanha).catch((e) => setErro(e.message))
    api(`/api/campanhas/${id}/rolagens`).then(setRolagens)
    api(`/api/campanhas/${id}/membros`).then(setMembros)
    api(`/api/campanhas/${id}/personagens`).then(setPersonagens)
    api(`/api/campanhas/${id}/sessoes`).then(setSessoes).catch(() => {})
  }

  function flashToast(r) {
    setToast(r)
    clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToast(null), 4500)
  }
  useEffect(() => () => clearTimeout(toastTimer.current), [])
  useEffect(() => {
    carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  // Tempo real (Fase 6): novas rolagens entram no topo + toast
  useEffect(
    () =>
      inscrever(`/topic/campanhas/${id}/rolagens`, (r) => {
        setRolagens((prev) => [r, ...prev])
        if (r && r.total != null) flashToast(r)
      }),
    [id],
  )

  async function rolar(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api(`/api/campanhas/${id}/rolagens`, {
        method: 'POST',
        body: { expressao: expr, rotulo, oculta, usuarioId: user?.id },
      })
    } catch (ex) {
      setErro(ex.message)
    }
  }

  async function criarSessao(e) {
    e.preventDefault()
    if (!sessaoForm.titulo.trim()) return
    setErro(null)
    try {
      await api(`/api/campanhas/${id}/sessoes`, {
        method: 'POST',
        body: { titulo: sessaoForm.titulo, inicio: sessaoForm.inicio || null },
      })
      setSessaoForm({ titulo: '', inicio: '' })
      api(`/api/campanhas/${id}/sessoes`).then(setSessoes)
    } catch (ex) { setErro(ex.message) }
  }
  async function apagarCampanha() {
    setErro(null)
    try {
      await api(`/api/campanhas/${id}`, { method: 'DELETE' })
      nav('/campanhas')
    } catch (ex) { setErro(ex.message); setConfirmarApagar(false) }
  }
  async function presenca(sessaoId, status) {
    setErro(null)
    try { await api(`/api/sessoes/${sessaoId}/presenca`, { method: 'POST', body: { usuarioId: user?.id, status } }) }
    catch (ex) { setErro(ex.message) }
  }

  async function criarConvite() {
    setErro(null)
    try {
      setConvite(
        await api(`/api/campanhas/${id}/convites`, {
          method: 'POST',
          body: { papel: 'JOGADOR', usuarioId: user?.id },
        }),
      )
    } catch (e) {
      setErro(e.message)
    }
  }

  if (!campanha) return <div className="center">Carregando…</div>
  return (
    <>
      <div className="page-head">
        <h1>{campanha.nome}</h1>
        <div className="spacer" />
        <Link className="tag" to={`/campanhas/${id}/escudo`}>
          Escudo do Mestre
        </Link>
        <Link className="tag" to={`/overlay/${id}`}>
          Overlay OBS
        </Link>
        <button className="ghost mini" onClick={() => setConfirmarApagar(true)}>Apagar campanha</button>
      </div>
      {erro && <p className="error">{erro}</p>}

      {confirmarApagar && (
        <div className="modal" onClick={() => setConfirmarApagar(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>Apagar campanha?</h2>
            <p className="muted">"{campanha.nome}" e seus vínculos serão removidos. Não dá pra desfazer.</p>
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button className="danger" onClick={apagarCampanha}>Sim, apagar</button>
              <button className="ghost" onClick={() => setConfirmarApagar(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      <div className="card">
        <h2>Rolar dados</h2>
        <form onSubmit={rolar} className="row">
          <div>
            <label>Expressão</label>
            <input value={expr} onChange={(e) => setExpr(e.target.value)} style={{ width: 120 }} />
          </div>
          <div style={{ flex: 1 }}>
            <label>Rótulo</label>
            <input value={rotulo} onChange={(e) => setRotulo(e.target.value)} />
          </div>
          <label className="row" style={{ alignSelf: 'end', gap: 6 }}>
            <input
              type="checkbox"
              style={{ width: 'auto' }}
              checked={oculta}
              onChange={(e) => setOculta(e.target.checked)}
            />{' '}
            oculta
          </label>
          <button style={{ alignSelf: 'end' }}>Rolar</button>
        </form>
      </div>

      <div className="card">
        <h2>Rolagens</h2>
        <table>
          <tbody>
            {rolagens.map((r) => (
              <tr key={r.id}>
                <td>{r.rotulo || r.expressao}</td>
                <td className="muted">{r.detalhe || (r.oculta ? '🎲 oculta' : '')}</td>
                <td
                  className="stat"
                  style={{
                    color: r.critico ? 'var(--crit)' : r.falhaCritica ? 'var(--fumble)' : 'inherit',
                  }}
                >
                  {r.total ?? '—'}
                  {r.critico ? ' ✦' : ''}
                  {r.falhaCritica ? ' ✗' : ''}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2>Membros</h2>
        <div className="row">
          {membros.map((m) => (
            <span key={m.id} className="tag">
              #{m.usuarioId} {m.papel}
            </span>
          ))}
        </div>
        <div style={{ marginTop: 10 }}>
          <button onClick={criarConvite}>Gerar convite</button>
          {convite && (
            <p className="muted">
              Código: <b>{convite.codigo}</b> (papel {convite.papel})
            </p>
          )}
        </div>
      </div>

      <div className="card">
        <h2>Sessões</h2>
        <div className="lista-vert">
          {sessoes.map((s) => (
            <div key={s.id} className="sessao-row">
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="row" style={{ gap: 8 }}>
                  <b>{s.titulo}</b>
                  <span className="tag">{s.status}</span>
                  {s.inicio && <span className="muted" style={{ fontSize: '.78rem' }}>{dataHora(s.inicio)}</span>}
                </div>
                {s.descricao && <div className="muted" style={{ fontSize: '.82rem' }}>{s.descricao}</div>}
              </div>
              <div className="row" style={{ gap: 4 }}>
                <button className="ghost mini" onClick={() => presenca(s.id, 'CONFIRMADO')} title="Confirmar">✓</button>
                <button className="ghost mini" onClick={() => presenca(s.id, 'TALVEZ')} title="Talvez">?</button>
                <button className="ghost mini" onClick={() => presenca(s.id, 'RECUSADO')} title="Recusar">✕</button>
              </div>
            </div>
          ))}
          {!sessoes.length && <span className="muted">Nenhuma sessão agendada.</span>}
        </div>
        <form onSubmit={criarSessao} className="row" style={{ gap: 8, marginTop: 12 }}>
          <div style={{ flex: 1, minWidth: 140 }}>
            <label>Título</label>
            <input value={sessaoForm.titulo} onChange={(e) => setSessaoForm((s) => ({ ...s, titulo: e.target.value }))} />
          </div>
          <div>
            <label>Início</label>
            <input type="datetime-local" value={sessaoForm.inicio}
              onChange={(e) => setSessaoForm((s) => ({ ...s, inicio: e.target.value }))} />
          </div>
          <button style={{ alignSelf: 'end' }}>Agendar</button>
        </form>
      </div>

      <h2 style={{ marginTop: 18 }}>Personagens na campanha</h2>
      {personagens.length === 0 && <p className="muted">Nenhum personagem vinculado.</p>}
      <div className="grid">
        {personagens.map((cp) => (
          <Link key={cp.id} to={`/personagens/${cp.personagemId}`} className="entity-card">
            <div className="av">{(cp.personagemNome || '?').charAt(0).toUpperCase()}</div>
            <div className="body">
              <div className="name">{cp.personagemNome}</div>
              <div className="sub">Ver ficha →</div>
            </div>
          </Link>
        ))}
      </div>

      {toast && (
        <div className={`roll-toast ${toast.critico ? 'crit' : toast.falhaCritica ? 'fumble' : ''}`}>
          <div className="muted">{toast.rotulo || toast.expressao}</div>
          <div className="rt-total">
            {toast.total}{toast.critico ? ' ✦' : ''}{toast.falhaCritica ? ' ✗' : ''}
          </div>
          {toast.detalhe && <div className="muted" style={{ fontSize: '.75rem' }}>{toast.detalhe}</div>}
        </div>
      )}
    </>
  )
}
