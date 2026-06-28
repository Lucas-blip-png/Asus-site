import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'

const fmtData = (iso) => {
  try {
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' })
  } catch {
    return ''
  }
}

const ABAS = ['Agentes', 'Jogadores', 'Sessões', 'Rolagens']

export default function Campanha() {
  const { id } = useParams()
  const nav = useNavigate()
  const { user } = useAuth()
  const [aba, setAba] = useState('Agentes')
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
  // Sobe uma capa (asset) e vincula na campanha.
  async function trocarCapa(file) {
    if (!file || !campanha) return
    setErro(null)
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('tipo', 'CAPA_CAMPANHA')
      const asset = await api(`/api/organizacoes/${campanha.organizacaoId}/assets`, { method: 'POST', body: form })
      await api(`/api/campanhas/${id}`, { method: 'PUT', body: { capaAssetId: String(asset.id) } })
      carregar()
    } catch (ex) { setErro(ex.message) }
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
      {/* Barra de ações (estilo CRIS) */}
      <div className="camp-actions">
        <label className="act" title="Trocar a capa da campanha">
          🖼 Foto de Capa
          <input type="file" accept="image/*" style={{ display: 'none' }}
            onChange={(e) => trocarCapa(e.target.files[0])} />
        </label>
        <button className="act" onClick={criarConvite}>✉ Convidar Jogadores</button>
        <Link className="act" to={`/campanhas/${id}/escudo`}>🛡 Escudo do Mestre</Link>
        <Link className="act" to={`/overlay/${id}`}>📺 Overlay OBS</Link>
        <button className="act danger" onClick={() => setConfirmarApagar(true)}>🗑 Apagar campanha</button>
      </div>

      <h1 style={{ margin: '4px 0 6px' }}>{campanha.nome}</h1>
      {campanha.descricao && <p className="muted" style={{ marginTop: 0 }}>{campanha.descricao}</p>}

      {/* Capa */}
      {campanha.capaAssetId ? (
        <div className="camp-cover" style={{ backgroundImage: `url(/api/assets/${campanha.capaAssetId}/conteudo)` }} />
      ) : (
        <label className="camp-cover placeholder" title="Adicionar capa">
          <span className="ini">{(campanha.nome || '?').charAt(0).toUpperCase()}</span>
          <span className="hint">＋ Adicionar capa</span>
          <input type="file" accept="image/*" style={{ display: 'none' }}
            onChange={(e) => trocarCapa(e.target.files[0])} />
        </label>
      )}

      {convite && (
        <p className="muted">Código de convite: <b>{convite.codigo}</b> (papel {convite.papel})</p>
      )}
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

      {/* Abas */}
      <div className="abas" style={{ marginTop: 18 }}>
        {ABAS.map((x) => (
          <button key={x} className={aba === x ? 'ativo' : undefined} onClick={() => setAba(x)}>
            {x}
            {x === 'Agentes' && personagens.length > 0 ? ` (${personagens.length})` : ''}
            {x === 'Jogadores' && membros.length > 0 ? ` (${membros.length})` : ''}
          </button>
        ))}
      </div>

      {aba === 'Agentes' && (
        <>
          {personagens.length === 0 && <p className="muted">Nenhum personagem vinculado.</p>}
          <div className="grid">
            {personagens.map((cp) => (
              <div key={cp.id} className="entity-card">
                <div className="av"
                  style={cp.avatarAssetId ? { backgroundImage: `url(/api/assets/${cp.avatarAssetId}/conteudo)` } : undefined}>
                  {!cp.avatarAssetId && (cp.personagemNome || '?').charAt(0).toUpperCase()}
                </div>
                <div className="body">
                  <div className="name">
                    {cp.personagemNome}
                    <Link to={`/personagens/${cp.personagemId}`} className="card-gear" title="Abrir ficha">⚙</Link>
                  </div>
                  <div className="sub">
                    {[cp.personagemClasse, cp.nivel ? `Nv ${cp.nivel}` : null].filter(Boolean).join(' · ') || '—'}
                  </div>
                  {cp.adicionadoEm && (
                    <div className="sub" style={{ fontSize: '.72rem' }}>Na campanha desde {fmtData(cp.adicionadoEm)}</div>
                  )}
                  <div className="foot">
                    <Link to={`/personagens/${cp.personagemId}`}><button className="mini">Acessar Ficha</button></Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {aba === 'Jogadores' && (
        <div className="card">
          <div className="row" style={{ flexWrap: 'wrap', gap: 8 }}>
            {membros.map((m) => (
              <span key={m.id} className="tag">#{m.usuarioId} {m.papel}</span>
            ))}
            {!membros.length && <span className="muted">Nenhum jogador ainda.</span>}
          </div>
          <div style={{ marginTop: 12 }}>
            <button onClick={criarConvite}>Gerar convite</button>
            {convite && (
              <p className="muted">Código: <b>{convite.codigo}</b> (papel {convite.papel})</p>
            )}
          </div>
        </div>
      )}

      {aba === 'Sessões' && (
        <div className="card">
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
      )}

      {aba === 'Rolagens' && (
        <>
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
                <input type="checkbox" style={{ width: 'auto' }} checked={oculta}
                  onChange={(e) => setOculta(e.target.checked)} /> oculta
              </label>
              <button style={{ alignSelf: 'end' }}>Rolar</button>
            </form>
          </div>

          <div className="card">
            <h2>Histórico</h2>
            <table>
              <tbody>
                {rolagens.map((r) => (
                  <tr key={r.id}>
                    <td>{r.rotulo || r.expressao}</td>
                    <td className="muted">{r.detalhe || (r.oculta ? '🎲 oculta' : '')}</td>
                    <td className="stat"
                      style={{ color: r.critico ? 'var(--crit)' : r.falhaCritica ? 'var(--fumble)' : 'inherit' }}>
                      {r.total ?? '—'}{r.critico ? ' ✦' : ''}{r.falhaCritica ? ' ✗' : ''}
                    </td>
                  </tr>
                ))}
                {!rolagens.length && <tr><td className="muted">Nenhuma rolagem ainda.</td></tr>}
              </tbody>
            </table>
          </div>
        </>
      )}

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
