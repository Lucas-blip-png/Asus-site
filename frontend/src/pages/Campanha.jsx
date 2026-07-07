import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'
import { inscrever } from '../ws.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'
import ResultadosPanel from '../components/ResultadosPanel.jsx'

const fmtData = (iso) => {
  try {
    return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' })
  } catch {
    return ''
  }
}

const ABAS = ['Agentes', 'Jogadores', 'Sessões', 'Combates']

const d20 = () => 1 + Math.floor(Math.random() * 20)

// Rastreador de iniciativa de um combate.
function CombateTracker({ combate, personagens, onClose, onMudou }) {
  const cid = combate.id
  const [c, setC] = useState(combate)
  const [parts, setParts] = useState([])
  const [erro, setErro] = useState(null)
  const [selAgente, setSelAgente] = useState('')
  const [ameaca, setAmeaca] = useState({ nome: '', iniciativa: '', pvMax: '' })
  const [bestiario, setBestiario] = useState([])
  const [selBicho, setSelBicho] = useState('')
  const [condForm, setCondForm] = useState({ pid: null, nome: '', turnos: '' })

  function recarregar() {
    api(`/api/combates/${cid}`).then(setC).catch(() => {})
    api(`/api/combates/${cid}/participantes`).then(setParts).catch(() => {})
  }
  useEffect(() => { recarregar() }, [cid]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { api('/api/bestiario').then(setBestiario).catch(() => {}) }, [])

  async function proximo() {
    try { setC(await api(`/api/combates/${cid}/proximo`, { method: 'POST' })); onMudou?.() }
    catch (e) { setErro(e.message) }
  }
  async function reiniciar() {
    try { setC(await api(`/api/combates/${cid}/reset`, { method: 'POST' })); onMudou?.() }
    catch (e) { setErro(e.message) }
  }
  async function addAgente() {
    if (!selAgente) return
    setErro(null)
    try {
      const cp = personagens.find((x) => String(x.personagemId) === String(selAgente))
      let pvMax = 0, pvAtual = 0
      try {
        const ficha = await api(`/api/personagens/${selAgente}`)
        pvMax = ficha?.status?.pvMax ?? 0
        pvAtual = ficha?.status?.pvAtual ?? pvMax
      } catch { /* segue com 0 */ }
      await api(`/api/combates/${cid}/participantes`, {
        method: 'POST',
        body: {
          nome: cp?.personagemNome || 'Agente',
          personagemId: Number(selAgente),
          avatarAssetId: cp?.avatarAssetId ?? null,
          pvMax, pvAtual, iniciativa: d20(), inimigo: false,
        },
      })
      setSelAgente('')
      recarregar()
    } catch (e) { setErro(e.message) }
  }
  async function addAmeaca() {
    if (!ameaca.nome.trim()) return
    setErro(null)
    try {
      await api(`/api/combates/${cid}/participantes`, {
        method: 'POST',
        body: {
          nome: ameaca.nome.trim(),
          iniciativa: ameaca.iniciativa === '' ? d20() : Number(ameaca.iniciativa),
          pvMax: Number(ameaca.pvMax) || 0,
          inimigo: true,
        },
      })
      setAmeaca({ nome: '', iniciativa: '', pvMax: '' })
      recarregar()
    } catch (e) { setErro(e.message) }
  }
  async function addBicho() {
    if (!selBicho) return
    setErro(null)
    try {
      const b = bestiario.find((x) => String(x.id) === String(selBicho))
      if (!b) return
      await api(`/api/combates/${cid}/participantes`, {
        method: 'POST',
        body: { nome: b.nome, pvMax: b.pv || 0, pvAtual: b.pv || 0, iniciativa: d20(), inimigo: true },
      })
      setSelBicho('')
      recarregar()
    } catch (e) { setErro(e.message) }
  }
  async function patchPart(pid, body) {
    try { await api(`/api/participantes/${pid}`, { method: 'PUT', body }); recarregar() }
    catch (e) { setErro(e.message) }
  }
  async function removerPart(pid) {
    try { await api(`/api/participantes/${pid}`, { method: 'DELETE' }); recarregar() }
    catch (e) { setErro(e.message) }
  }

  // ----- condições / efeitos -----
  const parseCond = (p) => { try { return JSON.parse(p.condicoes || '[]') } catch { return [] } }
  async function addCond(p) {
    if (!condForm.nome.trim()) return
    const lista = [...parseCond(p), { nome: condForm.nome.trim(), turnos: Number(condForm.turnos) || 0 }]
    setCondForm({ pid: null, nome: '', turnos: '' })
    await patchPart(p.id, { condicoes: JSON.stringify(lista) })
  }
  async function tickCond(p, idx, delta) {
    const lista = parseCond(p)
    const cd = lista[idx]
    if (!cd) return
    const t = (cd.turnos || 0) + delta
    if (t <= 0 && delta < 0) lista.splice(idx, 1)
    else lista[idx] = { ...cd, turnos: t }
    await patchPart(p.id, { condicoes: JSON.stringify(lista) })
  }
  async function removeCond(p, idx) {
    const lista = parseCond(p)
    lista.splice(idx, 1)
    await patchPart(p.id, { condicoes: JSON.stringify(lista) })
  }

  const disponiveis = personagens.filter(
    (cp) => !parts.some((p) => String(p.personagemId) === String(cp.personagemId)),
  )

  return (
    <div className="card">
      <div className="row" style={{ alignItems: 'center', gap: 10 }}>
        <h2 style={{ margin: 0 }}>{c.nome}</h2>
        <span className="tag">Rodada {c.rodada}</span>
        <div className="spacer" />
        <button className="mini" onClick={proximo}>▶ Próximo turno</button>
        <button className="ghost mini" onClick={reiniciar} title="Voltar para a rodada 1">↺</button>
        <button className="ghost mini" onClick={onClose}>Fechar</button>
      </div>
      {erro && <p className="error">{erro}</p>}

      <div className="combate-lista">
        {parts.map((p, i) => (
          <div key={p.id} className={`combate-row${i === c.turnoAtual ? ' ativo' : ''}${p.inimigo ? ' inimigo' : ''}`}>
            <span className="ini" title="Iniciativa">{p.iniciativa}</span>
            <div className="av-mini"
              style={p.avatarAssetId ? { backgroundImage: `url(/api/assets/${p.avatarAssetId}/conteudo)` } : undefined}>
              {!p.avatarAssetId && (p.nome || '?').charAt(0).toUpperCase()}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="nm">{i === c.turnoAtual ? '▶ ' : ''}{p.nome}</div>
              <div className="muted" style={{ fontSize: '.72rem' }}>{p.inimigo ? 'Ameaça' : 'Agente'}</div>
              {parseCond(p).length > 0 && (
                <div className="cond-chips">
                  {parseCond(p).map((cd, ci) => (
                    <span key={ci} className="cond-chip">
                      <button className="t" title="Clique: −1 turno" onClick={() => tickCond(p, ci, -1)}>
                        {cd.nome}{cd.turnos ? ` ${cd.turnos}` : ''}
                      </button>
                      <button className="x" title="Remover" onClick={() => removeCond(p, ci)}>✕</button>
                    </span>
                  ))}
                </div>
              )}
              {condForm.pid === p.id && (
                <div className="row" style={{ gap: 4, marginTop: 5 }}>
                  <input autoFocus placeholder="Efeito" value={condForm.nome} style={{ flex: 1, minWidth: 0 }}
                    onChange={(e) => setCondForm((s) => ({ ...s, nome: e.target.value }))}
                    onKeyDown={(e) => { if (e.key === 'Enter') addCond(p) }} />
                  <input type="number" placeholder="Turnos" style={{ width: 70 }} value={condForm.turnos}
                    onChange={(e) => setCondForm((s) => ({ ...s, turnos: e.target.value }))}
                    onKeyDown={(e) => { if (e.key === 'Enter') addCond(p) }} />
                  <button className="ghost mini" onClick={() => addCond(p)}>✓</button>
                  <button className="ghost mini" onClick={() => setCondForm({ pid: null, nome: '', turnos: '' })}>✕</button>
                </div>
              )}
            </div>
            <div className="row" style={{ gap: 4, alignItems: 'center' }}>
              <button className="ghost mini" title="-5 PV" onClick={() => patchPart(p.id, { pvAtual: Math.max(0, p.pvAtual - 5) })}>«</button>
              <button className="ghost mini" title="-1 PV" onClick={() => patchPart(p.id, { pvAtual: Math.max(0, p.pvAtual - 1) })}>‹</button>
              <b className="stat" style={{ minWidth: 54, textAlign: 'center' }}>{p.pvAtual}/{p.pvMax}</b>
              <button className="ghost mini" title="+1 PV" onClick={() => patchPart(p.id, { pvAtual: Math.min(p.pvMax || Infinity, p.pvAtual + 1) })}>›</button>
              <button className="ghost mini" title="Rolar iniciativa" onClick={() => patchPart(p.id, { iniciativa: d20() })}>🎲</button>
              <button className="ghost mini" title="Adicionar efeito/condição"
                onClick={() => setCondForm({ pid: p.id, nome: '', turnos: '' })}>＋ef</button>
              <button className="ghost mini" title="Remover" onClick={() => removerPart(p.id)}>✕</button>
            </div>
          </div>
        ))}
        {!parts.length && <div className="muted">Nenhum participante. Adicione agentes e ameaças abaixo.</div>}
      </div>

      <div className="add-form" style={{ marginTop: 12 }}>
        <select value={selAgente} onChange={(e) => setSelAgente(e.target.value)} style={{ flex: '1 1 160px' }}>
          <option value="">— agente da campanha —</option>
          {disponiveis.map((cp) => (
            <option key={cp.personagemId} value={cp.personagemId}>{cp.personagemNome}</option>
          ))}
        </select>
        <button className="mini" onClick={addAgente}>+ Agente</button>
      </div>
      <div className="add-form">
        <select value={selBicho} onChange={(e) => setSelBicho(e.target.value)} style={{ flex: '1 1 160px' }}>
          <option value="">— ameaça do bestiário —</option>
          {bestiario.map((b) => (
            <option key={b.id} value={b.id}>{b.nome}{b.pv ? ` (PV ${b.pv})` : ''}</option>
          ))}
        </select>
        <button className="mini" onClick={addBicho}>+ Do bestiário</button>
      </div>
      <div className="add-form">
        <input placeholder="Ameaça / NPC avulso" value={ameaca.nome}
          onChange={(e) => setAmeaca((s) => ({ ...s, nome: e.target.value }))} />
        <input type="number" placeholder="Inic." style={{ maxWidth: 80 }} value={ameaca.iniciativa}
          onChange={(e) => setAmeaca((s) => ({ ...s, iniciativa: e.target.value }))} />
        <input type="number" placeholder="PV" style={{ maxWidth: 80 }} value={ameaca.pvMax}
          onChange={(e) => setAmeaca((s) => ({ ...s, pvMax: e.target.value }))} />
        <button className="mini" onClick={addAmeaca}>+ Avulso</button>
      </div>
    </div>
  )
}

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
  const [convite, setConvite] = useState(null)
  const [sessoes, setSessoes] = useState([])
  const [presencas, setPresencas] = useState({})
  const [sessaoForm, setSessaoForm] = useState({ titulo: '', inicio: '' })
  const [toast, setToast] = useState(null)
  const toastTimer = useRef(null)
  const [erro, setErro] = useState(null)
  // Adicionar personagem / editar campanha / combates
  const [addPersoOpen, setAddPersoOpen] = useState(false)
  const [orgPersonagens, setOrgPersonagens] = useState([])
  const [selAdd, setSelAdd] = useState('')
  const [editOpen, setEditOpen] = useState(false)
  const [editForm, setEditForm] = useState({ nome: '', descricao: '' })
  const [combates, setCombates] = useState([])
  const [combateAtivo, setCombateAtivo] = useState(null)
  const [anotacoesTxt, setAnotacoesTxt] = useState('')
  const [anotacoesSalvo, setAnotacoesSalvo] = useState(false)

  function carregar() {
    api(`/api/campanhas/${id}`).then(setCampanha).catch((e) => setErro(e.message))
    api(`/api/campanhas/${id}/rolagens`).then(setRolagens).catch(() => {})
    api(`/api/campanhas/${id}/membros`).then(setMembros).catch(() => {})
    api(`/api/campanhas/${id}/personagens`).then(setPersonagens).catch(() => {})
    api(`/api/campanhas/${id}/sessoes`).then(setSessoes).catch(() => {})
    api(`/api/campanhas/${id}/combates`).then(setCombates).catch(() => {})
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
  // Carrega as presenças de cada sessão (para mostrar quem confirmou).
  useEffect(() => {
    sessoes.forEach((s) =>
      api(`/api/sessoes/${s.id}/presencas`)
        .then((ps) => setPresencas((m) => ({ ...m, [s.id]: ps })))
        .catch(() => {}),
    )
  }, [sessoes])
  // Anotações são privadas: carrega via endpoint gateado, só se for o mestre.
  useEffect(() => {
    const mestre = !!user && (campanha?.mestreId === user.id || user.dono)
    if (mestre) {
      api(`/api/campanhas/${id}/anotacoes?usuarioId=${user.id}`)
        .then((r) => setAnotacoesTxt(r.anotacoes || ''))
        .catch(() => {})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [campanha?.id, user])

  // Tempo real (Fase 6): novas rolagens entram no topo + toast.
  // Deduplica por id: se quem rolou já inseriu a versão completa (rolagem privada),
  // a versão mascarada que chega pelo broadcast não sobrescreve.
  useEffect(
    () =>
      inscrever(`/topic/campanhas/${id}/rolagens`, (r) => {
        setRolagens((prev) => (prev.some((x) => x.id === r.id) ? prev : [r, ...prev]))
        if (r && r.total != null) flashToast(r)
      }),
    [id],
  )

  // Rolagem vinda do painel de Resultados (chat). privada => oculta (só o mestre vê o valor).
  async function rolarPainel(expressao, rot, privada) {
    const r = await api(`/api/campanhas/${id}/rolagens`, {
      method: 'POST',
      body: { expressao, rotulo: rot, oculta: !!privada, usuarioId: user?.id },
    })
    // Quem rolou vê o próprio resultado completo na hora, mesmo sendo privado.
    if (r && r.id != null) {
      setRolagens((prev) => [r, ...prev.filter((x) => x.id !== r.id)])
      if (r.total != null) flashToast(r)
    }
  }
  const ehMestre = !!user && (campanha?.mestreId === user.id || user.dono)

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
  const carregarPresencas = (sid) =>
    api(`/api/sessoes/${sid}/presencas`).then((ps) => setPresencas((m) => ({ ...m, [sid]: ps }))).catch(() => {})
  async function presenca(sessaoId, status) {
    setErro(null)
    try {
      await api(`/api/sessoes/${sessaoId}/presenca`, { method: 'POST', body: { usuarioId: user?.id, status } })
      carregarPresencas(sessaoId)
    } catch (ex) { setErro(ex.message) }
  }
  async function apagarSessao(sid) {
    setErro(null)
    try { await api(`/api/sessoes/${sid}`, { method: 'DELETE' }); api(`/api/campanhas/${id}/sessoes`).then(setSessoes) }
    catch (ex) { setErro(ex.message) }
  }
  async function notificarSessao(sid) {
    setErro(null)
    try { await api(`/api/sessoes/${sid}/notificar`, { method: 'POST' }); setErro('Presentes notificados ✓'); setTimeout(() => setErro(null), 2000) }
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

  // ----- adicionar personagem -----
  async function abrirAddPerso() {
    setErro(null)
    try {
      // Carrega os personagens do PRÓPRIO usuário (cada jogador tem sua organização),
      // não os do dono da campanha — assim quem entrou por convite adiciona os seus.
      const oid = await obterOrgId()
      const lista = await api(`/api/organizacoes/${oid}/personagens`).catch(() => [])
      setOrgPersonagens(lista || [])
      setSelAdd('')
      setAddPersoOpen(true)
    } catch (e) { setErro(e.message) }
  }
  async function adicionarPersonagem() {
    if (!selAdd) return
    setErro(null)
    try {
      await api(`/api/campanhas/${id}/personagens`, { method: 'POST', body: { personagemId: Number(selAdd) } })
      setAddPersoOpen(false)
      api(`/api/campanhas/${id}/personagens`).then(setPersonagens)
      setAba('Agentes')
    } catch (e) { setErro(e.message) }
  }

  // ----- editar campanha -----
  function abrirEdit() {
    setEditForm({ nome: campanha.nome || '', descricao: campanha.descricao || '' })
    setEditOpen(true)
  }
  async function salvarEdicao() {
    setErro(null)
    try {
      await api(`/api/campanhas/${id}`, {
        method: 'PUT',
        body: { nome: editForm.nome, descricao: editForm.descricao },
      })
      setEditOpen(false)
      api(`/api/campanhas/${id}`).then(setCampanha)
    } catch (e) { setErro(e.message) }
  }

  // ----- anotações (mestre) -----
  async function salvarAnotacoes() {
    setErro(null)
    try {
      await api(`/api/campanhas/${id}/anotacoes?usuarioId=${user?.id}`, {
        method: 'PUT',
        body: { anotacoes: anotacoesTxt },
      })
      setAnotacoesSalvo(true)
      setTimeout(() => setAnotacoesSalvo(false), 2000)
    } catch (e) { setErro(e.message) }
  }

  // ----- combates -----
  const recarregarCombates = () => api(`/api/campanhas/${id}/combates`).then(setCombates).catch(() => {})
  async function criarCombate() {
    setErro(null)
    try {
      const nome = `Combate ${combates.length + 1}`
      const c = await api(`/api/campanhas/${id}/combates`, { method: 'POST', body: { nome } })
      await recarregarCombates()
      setCombateAtivo(c)
      setAba('Combates')
    } catch (e) { setErro(e.message) }
  }
  async function apagarCombate(combateId) {
    setErro(null)
    try {
      await api(`/api/combates/${combateId}`, { method: 'DELETE' })
      if (combateAtivo?.id === combateId) setCombateAtivo(null)
      recarregarCombates()
    } catch (e) { setErro(e.message) }
  }

  if (!campanha) return <div className="center">Carregando…</div>
  const personagensDisponiveis = orgPersonagens.filter(
    (p) => !personagens.some((cp) => String(cp.personagemId) === String(p.id)),
  )
  return (
    <>
      {/* Barra de ações (estilo CRIS) */}
      <div className="camp-actions">
        <button className="act" onClick={abrirAddPerso}>＋ Adicionar Personagens</button>
        <Link className="act" to={`/overlay/${id}`}>📺 Overlay OBS</Link>
        {ehMestre ? (
          <>
            <label className="act" title="Trocar a capa da campanha">
              🖼 Foto de Capa
              <input type="file" accept="image/*" style={{ display: 'none' }}
                onChange={(e) => trocarCapa(e.target.files[0])} />
            </label>
            <button className="act" onClick={criarConvite}>✉ Convidar Jogadores</button>
            <button className="act" onClick={abrirEdit}>✎ Editar Campanha</button>
            <button className="act" onClick={criarCombate}>⚔ Criar Combate</button>
            <Link className="act" to={`/campanhas/${id}/escudo`}>🛡 Escudo do Mestre</Link>
            <button className="act danger" onClick={() => setConfirmarApagar(true)}>🗑 Apagar campanha</button>
          </>
        ) : (
          <span className="tag" style={{ alignSelf: 'center' }}>Você é jogador nesta campanha</span>
        )}
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
        {[...ABAS, ...(ehMestre ? ['Anotações'] : [])].map((x) => (
          <button key={x} className={aba === x ? 'ativo' : undefined} onClick={() => setAba(x)}>
            {x}
            {x === 'Agentes' && personagens.length > 0 ? ` (${personagens.length})` : ''}
            {x === 'Jogadores' && membros.length > 0 ? ` (${membros.length})` : ''}
            {x === 'Combates' && combates.length > 0 ? ` (${combates.length})` : ''}
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
            {sessoes.map((s) => {
              const ps = presencas[s.id] || []
              const minha = ps.find((x) => x.usuarioId === user?.id)?.status
              const conta = (st) => ps.filter((x) => x.status === st).length
              return (
                <div key={s.id} className="sessao-row">
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="row" style={{ gap: 8 }}>
                      <b>{s.titulo}</b>
                      <span className="tag">{s.status}</span>
                      {s.inicio && <span className="muted" style={{ fontSize: '.78rem' }}>{dataHora(s.inicio)}</span>}
                    </div>
                    {s.descricao && <div className="muted" style={{ fontSize: '.82rem' }}>{s.descricao}</div>}
                    <div className="muted" style={{ fontSize: '.74rem', marginTop: 2 }}>
                      ✓ {conta('CONFIRMADO')} · ? {conta('TALVEZ')} · ✕ {conta('RECUSADO')}
                      {minha && <span> · você: {minha === 'CONFIRMADO' ? '✓ vai' : minha === 'TALVEZ' ? '? talvez' : '✕ não vai'}</span>}
                    </div>
                  </div>
                  <div className="row" style={{ gap: 4, alignItems: 'center' }}>
                    <button className={`ghost mini${minha === 'CONFIRMADO' ? ' ativo' : ''}`} onClick={() => presenca(s.id, 'CONFIRMADO')} title="Confirmar">✓</button>
                    <button className={`ghost mini${minha === 'TALVEZ' ? ' ativo' : ''}`} onClick={() => presenca(s.id, 'TALVEZ')} title="Talvez">?</button>
                    <button className={`ghost mini${minha === 'RECUSADO' ? ' ativo' : ''}`} onClick={() => presenca(s.id, 'RECUSADO')} title="Recusar">✕</button>
                    {ehMestre && (
                      <>
                        <button className="ghost mini" onClick={() => notificarSessao(s.id)} title="Avisar quem confirmou">🔔</button>
                        <button className="ghost mini" onClick={() => apagarSessao(s.id)} title="Apagar sessão">🗑</button>
                      </>
                    )}
                  </div>
                </div>
              )
            })}
            {!sessoes.length && <span className="muted">Nenhuma sessão agendada.</span>}
          </div>
          {ehMestre && (
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
          )}
        </div>
      )}

      {aba === 'Combates' && (
        <>
          {ehMestre && (
            <div className="row" style={{ marginBottom: 12, gap: 8 }}>
              <button onClick={criarCombate}>⚔ Criar Combate</button>
            </div>
          )}
          {combateAtivo ? (
            <CombateTracker
              combate={combateAtivo}
              personagens={personagens}
              onClose={() => { setCombateAtivo(null); recarregarCombates() }}
              onMudou={recarregarCombates}
            />
          ) : (
            <div className="lista-vert">
              {combates.map((c) => (
                <div key={c.id} className="sessao-row">
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="row" style={{ gap: 8 }}>
                      <b>{c.nome}</b>
                      <span className="tag">Rodada {c.rodada}</span>
                    </div>
                  </div>
                  <div className="row" style={{ gap: 4 }}>
                    <button className="mini" onClick={() => setCombateAtivo(c)}>Abrir</button>
                    <button className="ghost mini" onClick={() => apagarCombate(c.id)} title="Apagar">✕</button>
                  </div>
                </div>
              ))}
              {!combates.length && <span className="muted">Nenhum combate. Clique em “Criar Combate”.</span>}
            </div>
          )}
        </>
      )}


      {addPersoOpen && (
        <div className="modal" onClick={() => setAddPersoOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 440 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Adicionar personagem</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setAddPersoOpen(false)}>✕</button>
            </div>
            <label style={{ marginTop: 8 }}>Personagem</label>
            <select value={selAdd} onChange={(e) => setSelAdd(e.target.value)}>
              <option value="">— escolha um personagem —</option>
              {personagensDisponiveis.map((p) => (
                <option key={p.id} value={p.id}>{p.nome}</option>
              ))}
            </select>
            {!personagensDisponiveis.length && (
              <p className="muted" style={{ fontSize: '.82rem' }}>
                Todos os seus personagens já estão na campanha (ou você ainda não criou nenhum).
              </p>
            )}
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button disabled={!selAdd} onClick={adicionarPersonagem}>Adicionar</button>
              <button className="ghost" onClick={() => setAddPersoOpen(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {editOpen && (
        <div className="modal" onClick={() => setEditOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 480 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Editar campanha</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setEditOpen(false)}>✕</button>
            </div>
            <label style={{ marginTop: 8 }}>Nome</label>
            <input value={editForm.nome} onChange={(e) => setEditForm((s) => ({ ...s, nome: e.target.value }))} />
            <label>Descrição</label>
            <textarea value={editForm.descricao}
              onChange={(e) => setEditForm((s) => ({ ...s, descricao: e.target.value }))} />
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button onClick={salvarEdicao}>Salvar</button>
              <button className="ghost" onClick={() => setEditOpen(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {aba === 'Anotações' && ehMestre && (
        <div className="card">
          <div className="row" style={{ alignItems: 'center' }}>
            <h2 style={{ margin: 0 }}>Anotações do mestre</h2>
            <span className="muted" style={{ fontSize: '.78rem' }}>· aba visível só para o mestre</span>
            <div className="spacer" />
            {anotacoesSalvo && <span className="tag">salvo ✓</span>}
            <button className="mini" onClick={salvarAnotacoes}>Salvar</button>
          </div>
          <textarea
            value={anotacoesTxt}
            onChange={(e) => setAnotacoesTxt(e.target.value)}
            placeholder="Tramas, NPCs, segredos, pistas, ganchos de sessão…"
            style={{ minHeight: 320, marginTop: 10 }}
          />
        </div>
      )}

      <ResultadosPanel rolagens={rolagens} onRolar={rolarPainel} ehMestre={ehMestre} />

      {toast && (
        <div className={`roll-toast ${toast.critico ? 'crit' : toast.falhaCritica ? 'fumble' : ''}`}>
          <div className="muted">{toast.personagemNome ? `${toast.personagemNome} · ` : ''}{toast.rotulo || toast.expressao}</div>
          <div className="rt-total">
            {toast.total}{toast.critico ? ' ✦' : ''}{toast.falhaCritica ? ' ✗' : ''}
          </div>
          {toast.detalhe && <div className="muted" style={{ fontSize: '.75rem' }}>{toast.detalhe}</div>}
        </div>
      )}
    </>
  )
}
