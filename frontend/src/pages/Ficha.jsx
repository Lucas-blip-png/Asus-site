import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'
import { useAuth } from '../auth.jsx'
import Heptagono from '../components/Heptagono.jsx'
import ResultadosPanel from '../components/ResultadosPanel.jsx'

const ABAS = ['Combate', 'Habilidades', 'Magias', 'Inventário', 'Descrição']
const BARRAS = [['Vida', 'vida', 'pv'], ['Mana', 'mana', 'pm'], ['Energia', 'energia', 'pe']]
const ATRIBS = [
  ['forca', 'For'], ['constituicao', 'Con'], ['destreza', 'Des'], ['agilidade', 'Agi'],
  ['inteligencia', 'Int'], ['sabedoria', 'Sab'], ['carisma', 'Car'],
]
const CAMPOS_DESC = [
  ['anotacoes', 'Anotações'], ['aparencia', 'Aparência'], ['personalidade', 'Personalidade'],
  ['historico', 'Histórico'], ['objetivo', 'Objetivo'],
]
const vazioBase = { forca: 0, constituicao: 0, destreza: 0, agilidade: 0, inteligencia: 0, sabedoria: 0, carisma: 0 }
// Formata espaços/carga aceitando meio-espaço (0,5) no padrão pt-BR.
const fmtEsp = (n) => String(Math.round((Number(n) || 0) * 100) / 100).replace('.', ',')
// Nome legível de uma lista de códigos de classe ("CAVALEIRO,BARBARO" -> "Cavaleiro, Barbaro").
const fmtClasses = (cods) => (cods || '').split(',').map((c) => c.trim()).filter(Boolean)
  .map((c) => c.split('_').map((w) => (w ? w[0] + w.slice(1).toLowerCase() : '')).join(' ')).join(', ')

function ItemInvRow({ it, onQtd, onEquip, onDelete, onEdit, onCombate }) {
  const [open, setOpen] = useState(false)
  const resumo = [it.dano && `Dano ${it.dano}`, it.bonusDefesa != null && `Defesa +${it.bonusDefesa}`].filter(Boolean).join(' · ')
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{it.equipado ? '🛡️ ' : ''}{it.nome}</b>
        {resumo && <span className="sub">{resumo}</span>}
        <div className="spacer" />
        <span className="tag">{fmtEsp(it.espacos)}×{it.quantidade || 1} esp</span>
      </div>
      {open && (
        <div className="cris-body">
          <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
            {it.categoria && <span className="tag">{it.categoria}</span>}
            {it.dano && <span className="tag">Dano {it.dano}{it.critico ? ` (${it.critico})` : ''}</span>}
            {it.bonusDefesa != null && <span className="tag">Defesa +{it.bonusDefesa}</span>}
            {it.alcance && <span className="tag">{it.alcance}</span>}
            <span className="tag">Espaços {fmtEsp(it.espacos)}</span>
          </div>
          {it.efeito && <p className="muted" style={{ fontSize: '.82rem', marginTop: 7 }}>{it.efeito}</p>}
          <div className="row" style={{ gap: 10, marginTop: 9, alignItems: 'center' }}>
            <span className="step">
              <button className="ghost mini" onClick={() => onQtd(it, -1)}>−</button>
              <b className="stat">{it.quantidade || 1}</b>
              <button className="ghost mini" onClick={() => onQtd(it, +1)}>+</button>
            </span>
            <label className="muted" style={{ fontSize: '.75rem' }}>
              <input type="checkbox" checked={!!it.equipado} onChange={() => onEquip(it)} /> equipado
            </label>
            <div className="spacer" />
            {it.dano && <button className="ghost mini" title="Criar ataque na aba Combate" onClick={() => onCombate(it)}>⚔ Combate</button>}
            <button className="ghost mini" onClick={() => onEdit(it)}>Editar</button>
            <button className="ghost mini" onClick={() => onDelete(it.id)}>Remover</button>
          </div>
        </div>
      )}
    </div>
  )
}

function AtaqueRow({ a, onEdit, onDelete }) {
  const [open, setOpen] = useState(false)
  const resumo = [a.dano && `Dano ${a.dano}`, a.critico && `Crít ${a.critico}`].filter(Boolean).join(' · ')
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{a.nome}</b>
        {resumo && <span className="sub">{resumo}</span>}
        <div className="spacer" />
        {a.alcance && <span className="tag">{a.alcance}</span>}
      </div>
      {open && (
        <div className="cris-body">
          <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
            {a.dano && <span className="tag">Dano {a.dano}</span>}
            {a.critico && <span className="tag">Crítico {a.critico}</span>}
            {a.alcance && <span className="tag">{a.alcance}</span>}
            {a.pericia && <span className="tag">{a.pericia}</span>}
          </div>
          {a.efeito && <p className="muted" style={{ fontSize: '.82rem', marginTop: 7 }}>{a.efeito}</p>}
          <div className="row" style={{ gap: 10, marginTop: 9, alignItems: 'center' }}>
            <div className="spacer" />
            <button className="ghost mini" onClick={() => onEdit(a)}>Editar</button>
            <button className="ghost mini" onClick={() => onDelete(a.id)}>Remover</button>
          </div>
        </div>
      )}
    </div>
  )
}

function FeiticoRow({ f, onEdit, onDelete }) {
  const [open, setOpen] = useState(false)
  const resumo = [f.circulo && `${f.circulo}º círculo`, f.custoPm && `${f.custoPm} PM`].filter(Boolean).join(' · ')
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{f.nome}</b>
        {resumo && <span className="sub">{resumo}</span>}
        <div className="spacer" />
        {f.alcance && <span className="tag">{f.alcance}</span>}
      </div>
      {open && (
        <div className="cris-body">
          <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
            {f.circulo ? <span className="tag">{f.circulo}º círculo</span> : null}
            {f.custoPm ? <span className="tag">{f.custoPm} PM</span> : null}
            {f.alcance && <span className="tag">{f.alcance}</span>}
            {f.duracao && <span className="tag">{f.duracao}</span>}
          </div>
          {f.efeito && <p className="muted" style={{ fontSize: '.82rem', marginTop: 7 }}>{f.efeito}</p>}
          <div className="row" style={{ gap: 10, marginTop: 9, alignItems: 'center' }}>
            <div className="spacer" />
            <button className="ghost mini" onClick={() => onEdit(f)}>Editar</button>
            <button className="ghost mini" onClick={() => onDelete(f.id)}>Remover</button>
          </div>
        </div>
      )}
    </div>
  )
}

function HabRow({ h, onEdit, onDelete }) {
  const [open, setOpen] = useState(false)
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{h.nome}</b>
        {h.tipo && <span className="sub">{h.tipo}</span>}
        <div className="spacer" />
        {h.custo > 0 && <span className="tag">{h.custo} {h.custoTipo}</span>}
      </div>
      {open && (
        <div className="cris-body">
          <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
            {h.tipo && <span className="tag">{h.tipo}</span>}
            {h.custo > 0 && <span className="tag">Custo {h.custo} {h.custoTipo}</span>}
            {h.classeCodigo && h.classeCodigo !== 'GERAL' && <span className="tag">{fmtClasses(h.classeCodigo)}</span>}
          </div>
          {h.efeito && <p className="muted" style={{ fontSize: '.82rem', marginTop: 7 }}>{h.efeito}</p>}
          <div className="row" style={{ gap: 10, marginTop: 9, alignItems: 'center' }}>
            <div className="spacer" />
            <button className="ghost mini" onClick={() => onEdit(h)}>Editar</button>
            <button className="ghost mini" onClick={() => onDelete(h.codigo)}>Remover</button>
          </div>
        </div>
      )}
    </div>
  )
}

export default function Ficha() {
  const { id } = useParams()
  const nav = useNavigate()
  const { user } = useAuth()
  const [campanhaAtiva, setCampanhaAtiva] = useState(null)
  const [rolagens, setRolagens] = useState([])
  const [p, setP] = useState(null)
  const [aba, setAba] = useState('Combate')
  const [erro, setErro] = useState(null)
  const [apagarOpen, setApagarOpen] = useState(false)
  const [apagarTxt, setApagarTxt] = useState('')
  const [treino, setTreino] = useState({})
  const [desc, setDesc] = useState({})
  const [base, setBase] = useState(vazioBase)
  const [nivelInput, setNivelInput] = useState('0')
  const [xpInput, setXpInput] = useState('0')
  const [statusInput, setStatusInput] = useState({ pvAtual: 0, pmAtual: 0, peAtual: 0 })
  const [editStatus, setEditStatus] = useState(null)
  const [levelUp, setLevelUp] = useState(null)
  const [habChosen, setHabChosen] = useState([])
  const [habDisp, setHabDisp] = useState([])
  const [habSel, setHabSel] = useState('')
  const [modalHab, setModalHab] = useState(false)
  const [habBusca, setHabBusca] = useState('')
  const [itens, setItens] = useState([])
  const [classesCat, setClassesCat] = useState([])
  const [ataques, setAtaques] = useState([])
  const [feiticos, setFeiticos] = useState([])
  const [novoAtaque, setNovoAtaque] = useState({ nome: '', dano: '', critico: '', alcance: '' })
  const [novoFeitico, setNovoFeitico] = useState({ nome: '', circulo: 1, custoPm: 0, alcance: '', efeito: '' })
  const [editAtaque, setEditAtaque] = useState(null)
  const [editFeitico, setEditFeitico] = useState(null)
  const [editHab, setEditHab] = useState(null)
  const [inventario, setInventario] = useState([])
  const [itemCat, setItemCat] = useState('')
  const [novoItem, setNovoItem] = useState({ nome: '', categoria: 'GERAL', espacos: 1, quantidade: 1 })
  const [editItem, setEditItem] = useState(null)
  const [outros, setOutros] = useState([])
  const [outrosBonus, setOutrosBonus] = useState({})
  const [novaPericia, setNovaPericia] = useState({ nome: '', atributo: 'FORCA' })
  const [criarHabOpen, setCriarHabOpen] = useState(false)
  const [novaHab, setNovaHab] = useState({ nome: '', tipo: 'PASSIVA', custo: 0, custoTipo: 'PE', efeito: '' })
  const [rolagem, setRolagem] = useState(null)
  const rolTimer = useRef(null)

  function toastRolagem(r) {
    setRolagem(r)
    clearTimeout(rolTimer.current)
    rolTimer.current = setTimeout(() => setRolagem(null), 5000)
  }
  // Rola 1d20 + modificador. Se o personagem está numa campanha, rola no servidor
  // (aparece no chat de Resultados de todos); senão, rola localmente.
  async function rolar(rotulo, mod = 0) {
    const m = Number(mod) || 0
    if (campanhaAtiva) {
      const expressao = '1d20' + (m > 0 ? `+${m}` : m < 0 ? `${m}` : '')
      try {
        const r = await api(`/api/campanhas/${campanhaAtiva.id}/rolagens`, {
          method: 'POST',
          body: { expressao, rotulo, personagemId: Number(id), usuarioId: user?.id },
        })
        toastRolagem({ rotulo, d: r.naturalD20 ?? r.total, mod: m, total: r.total, crit: !!r.critico, fumble: !!r.falhaCritica })
        return
      } catch { /* cai pro local */ }
    }
    const d = 1 + Math.floor(Math.random() * 20)
    toastRolagem({ rotulo, d, mod: m, total: d + m, crit: d === 20, fumble: d === 1 })
  }
  // Rolagem livre vinda do painel de Resultados (chat).
  async function rolarPainel(expressao, rot) {
    await api(`/api/campanhas/${campanhaAtiva.id}/rolagens`, {
      method: 'POST',
      body: { expressao, rotulo: rot, personagemId: Number(id), usuarioId: user?.id },
    })
  }
  useEffect(() => () => clearTimeout(rolTimer.current), [])

  // Descobre a campanha do personagem (para o chat de Resultados na ficha).
  useEffect(() => {
    api(`/api/personagens/${id}/campanhas`)
      .then((cs) => setCampanhaAtiva(cs && cs.length ? cs[0] : null))
      .catch(() => setCampanhaAtiva(null))
  }, [id])

  // Histórico + tempo real das rolagens da campanha ativa.
  useEffect(() => {
    if (!campanhaAtiva) { setRolagens([]); return undefined }
    api(`/api/campanhas/${campanhaAtiva.id}/rolagens`).then(setRolagens).catch(() => {})
    return inscrever(`/topic/campanhas/${campanhaAtiva.id}/rolagens`, (r) => setRolagens((prev) => [r, ...prev]))
  }, [campanhaAtiva])

  // Status em tempo real: se o mestre (ou outro) muda PV/PM/PE, a ficha atualiza sozinha.
  useEffect(() =>
    inscrever(`/topic/personagens/${id}/status`, (s) =>
      setP((prev) => (prev ? { ...prev, status: { ...prev.status, ...s } } : prev)),
    ), [id])

  function aplicar(d) {
    setP(d)
    setTreino(Object.fromEntries((d.pericias || []).filter((pe) => !pe.custom).map((pe) => [pe.codigo, pe.treino])))
    setOutrosBonus(Object.fromEntries((d.pericias || []).filter((pe) => !pe.custom).map((pe) => [pe.codigo, pe.outros || 0])))
    setOutros((d.pericias || []).filter((pe) => pe.custom).map((pe) => ({ nome: pe.nome, atributo: pe.atributoBase, treino: pe.treino })))
    setDesc({
      anotacoes: d.anotacoes || '', aparencia: d.aparencia || '', personalidade: d.personalidade || '',
      historico: d.historico || '', objetivo: d.objetivo || '',
    })
    setBase({ ...vazioBase, ...(d.atributosBase || {}) })
    setNivelInput(String(d.nivel ?? 0))
    setXpInput(String(d.xpAtual ?? 0))
    setStatusInput({ pvAtual: d.status.pvAtual, pmAtual: d.status.pmAtual, peAtual: d.status.peAtual })
  }

  async function carregar() {
    const d = await api(`/api/personagens/${id}`)
    aplicar(d)
    return d
  }

  useEffect(() => {
    carregar()
      .then((d) => {
        api(`/api/personagens/${id}/habilidades`).then(setHabChosen).catch(() => {})
        api(`/api/personagens/${id}/habilidades/disponiveis`).then(setHabDisp).catch(() => {})
        api('/api/sistemas/asus/itens').then(setItens).catch(() => {})
        api('/api/sistemas/asus/classes').then(setClassesCat).catch(() => {})
        api(`/api/personagens/${id}/ataques`).then(setAtaques).catch(() => {})
        api(`/api/personagens/${id}/feiticos`).then(setFeiticos).catch(() => {})
        api(`/api/personagens/${id}/inventario`).then(setInventario).catch(() => {})
      })
      .catch((e) => setErro(e.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  // ----- status (PV/PM/PE) -----
  async function ajustar(campo, delta) {
    try {
      const novo = Math.max(0, p.status[campo] + delta)
      await api(`/api/personagens/${id}/status`, { method: 'PATCH', body: { [campo]: novo } })
      carregar()
    } catch (e) { setErro(e.message) }
  }
  async function definirStatus(campo) {
    try {
      const novo = Math.max(0, Number(statusInput[campo]) || 0)
      await api(`/api/personagens/${id}/status`, { method: 'PATCH', body: { [campo]: novo } })
      carregar()
    } catch (e) { setErro(e.message) }
  }
  function abrirEditStatus(k, rot) {
    setEditStatus({ k, rot, atual: p.status[k + 'Atual'], max: p.status[k + 'Max'] })
  }
  async function salvarEditStatus() {
    if (!editStatus) return
    const { k } = editStatus
    try {
      await api(`/api/personagens/${id}/status`, {
        method: 'PATCH',
        body: {
          [k + 'Atual']: Math.max(0, Number(editStatus.atual) || 0),
          [k + 'Max']: Math.max(1, Number(editStatus.max) || 1),
        },
      })
      setEditStatus(null)
      carregar()
    } catch (e) { setErro(e.message) }
  }
  // Volta o teto a ser calculado automaticamente pelas regras (limpa o override).
  async function resetStatusMax() {
    if (!editStatus) return
    try {
      await api(`/api/personagens/${id}/status`, { method: 'PATCH', body: { [editStatus.k + 'Max']: 0 } })
      setEditStatus(null)
      carregar()
    } catch (e) { setErro(e.message) }
  }

  // ----- atributos: o valor FINAL (base + fixos da classe) não passa do teto do nível -----
  const tetoAtributo = (p && p.limiteAtributo > 0) ? p.limiteAtributo : 99
  const bonusClasse = (attr) => (p ? ((p.atributosFinais[attr] || 0) - (p.atributosBase[attr] || 0)) : 0)
  function setAtr(attr, delta) {
    const cap = tetoAtributo - bonusClasse(attr) // teto menos o bônus fixo da classe
    setBase((b) => {
      const atual = Number(b[attr]) || 0
      const novo = Math.max(0, Math.min(cap, atual + delta))
      return { ...b, [attr]: novo }
    })
  }
  async function salvarAtributos() {
    try {
      await api(`/api/personagens/${id}`, { method: 'PUT', body: { atributosBase: base } })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  // ----- nível / XP (level-up automático + popup) -----
  async function salvarProgresso() {
    try {
      const resp = await api(`/api/personagens/${id}/progresso`, {
        method: 'PATCH',
        body: { xpAtual: Number(xpInput) || 0, nivel: Number(nivelInput) || 0 },
      })
      aplicar(resp.personagem)
      if (resp.niveisGanhos && resp.niveisGanhos.length) setLevelUp(resp.niveisGanhos)
    } catch (e) { setErro(e.message) }
  }

  // ----- apagar a ficha (com confirmação por digitação) -----
  const palavraConfere = ['apagar', 'remover'].includes(apagarTxt.trim().toLowerCase())
  async function apagarFicha() {
    if (!palavraConfere) return
    try {
      await api(`/api/personagens/${id}`, { method: 'DELETE' })
      nav('/personagens')
    } catch (e) {
      setErro(e.message)
      setApagarOpen(false)
    }
  }

  // Overlay OBS por personagem: abre a página pública e copia a URL pro Browser Source.
  const [overlayCopiado, setOverlayCopiado] = useState(false)
  function abrirOverlay() {
    const url = `${window.location.origin}/overlay/ficha/${id}`
    try {
      navigator.clipboard?.writeText(url)
      setOverlayCopiado(true)
      setTimeout(() => setOverlayCopiado(false), 2500)
    } catch { /* ignora clipboard indisponivel */ }
    window.open(url, '_blank', 'noopener')
  }

  // ----- foto -----
  async function trocarFoto(file) {
    if (!file) return
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('tipo', 'AVATAR_PERSONAGEM')
      const asset = await api(`/api/organizacoes/${p.organizacaoId}/assets`, { method: 'POST', body: form })
      await api(`/api/personagens/${id}`, { method: 'PUT', body: { avatarAssetId: asset.id } })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  // Treino respeita o teto do atributo; sem orçamento/trava de pontos.
  const setTr = (cod, delta, cap) =>
    setTreino((t) => {
      const atual = t[cod] || 0
      const novo = Math.max(0, Math.min(cap, atual + delta))
      return { ...t, [cod]: novo }
    })
  // Bônus "Outros" por perícia: livre, sem teto.
  const setOutro = (cod, delta) =>
    setOutrosBonus((o) => ({ ...o, [cod]: Math.max(0, (o[cod] || 0) + delta) }))
  const setTrCustom = (idx, delta, cap) =>
    setOutros((arr) => arr.map((o, i) => (i === idx ? { ...o, treino: Math.max(0, Math.min(cap, o.treino + delta)) } : o)))
  const addOutro = () => {
    if (!novaPericia.nome.trim()) return
    setOutros((arr) => [...arr, { nome: novaPericia.nome.trim(), atributo: novaPericia.atributo, treino: 0 }])
    setNovaPericia({ nome: '', atributo: 'FORCA' })
  }
  const delOutro = (idx) => setOutros((arr) => arr.filter((_, i) => i !== idx))
  const siglaDe = (atributo) => (ATRIBS.find(([k]) => k === atributo.toLowerCase()) || [])[1] || atributo

  async function salvar(body) {
    try {
      await api(`/api/personagens/${id}`, { method: 'PUT', body })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  const recarregarAtaques = () => api(`/api/personagens/${id}/ataques`).then(setAtaques).catch(() => {})
  const recarregarFeiticos = () => api(`/api/personagens/${id}/feiticos`).then(setFeiticos).catch(() => {})

  async function addAtaque() {
    if (!novoAtaque.nome.trim()) return
    try {
      await api(`/api/personagens/${id}/ataques`, { method: 'POST', body: novoAtaque })
      setNovoAtaque({ nome: '', dano: '', critico: '', alcance: '' })
      recarregarAtaques()
    } catch (e) { setErro(e.message) }
  }
  async function delAtaque(aid) {
    try { await api(`/api/ataques/${aid}`, { method: 'DELETE' }); recarregarAtaques() } catch (e) { setErro(e.message) }
  }
  async function salvarEdicaoAtaque() {
    if (!editAtaque) return
    try {
      await api(`/api/ataques/${editAtaque.id}`, {
        method: 'PUT',
        body: {
          nome: editAtaque.nome,
          dano: editAtaque.dano || '',
          critico: editAtaque.critico || '',
          alcance: editAtaque.alcance || '',
          pericia: editAtaque.pericia || '',
          efeito: editAtaque.efeito || '',
        },
      })
      setEditAtaque(null)
      recarregarAtaques()
    } catch (e) { setErro(e.message) }
  }
  async function addFeitico() {
    if (!novoFeitico.nome.trim()) return
    try {
      await api(`/api/personagens/${id}/feiticos`, {
        method: 'POST',
        body: { ...novoFeitico, circulo: Number(novoFeitico.circulo) || null, custoPm: Number(novoFeitico.custoPm) || null },
      })
      setNovoFeitico({ nome: '', circulo: 1, custoPm: 0, alcance: '', efeito: '' })
      recarregarFeiticos()
    } catch (e) { setErro(e.message) }
  }
  async function delFeitico(fid) {
    try { await api(`/api/feiticos/${fid}`, { method: 'DELETE' }); recarregarFeiticos() } catch (e) { setErro(e.message) }
  }
  async function salvarEdicaoFeitico() {
    if (!editFeitico) return
    try {
      await api(`/api/feiticos/${editFeitico.id}`, {
        method: 'PUT',
        body: {
          nome: editFeitico.nome,
          circulo: editFeitico.circulo === '' || editFeitico.circulo == null ? null : Number(editFeitico.circulo),
          custoPm: editFeitico.custoPm === '' || editFeitico.custoPm == null ? null : Number(editFeitico.custoPm),
          alcance: editFeitico.alcance || '',
          duracao: editFeitico.duracao || '',
          efeito: editFeitico.efeito || '',
        },
      })
      setEditFeitico(null)
      recarregarFeiticos()
    } catch (e) { setErro(e.message) }
  }

  // ----- inventário (carga = Força x 2 espaços) -----
  const recarregarInv = () => { api(`/api/personagens/${id}/inventario`).then(setInventario).catch(() => {}); carregar() }
  async function addCatalogo() {
    if (!itemCat) return
    // Uma arma (item com dano) cria automaticamente um ataque na aba Combate — recarrega os dois.
    try { await api(`/api/personagens/${id}/inventario/do-catalogo/${itemCat}`, { method: 'POST' }); setItemCat(''); recarregarInv(); recarregarAtaques() }
    catch (e) { setErro(e.message) }
  }
  async function addItemProprio() {
    if (!novoItem.nome.trim()) return
    try {
      await api(`/api/personagens/${id}/inventario`, {
        method: 'POST',
        body: { ...novoItem, espacos: Number(novoItem.espacos) || 0, quantidade: Number(novoItem.quantidade) || 1 },
      })
      setNovoItem({ nome: '', categoria: 'GERAL', espacos: 1, quantidade: 1, dano: '', critico: '' })
      recarregarInv()
      recarregarAtaques()
    } catch (e) { setErro(e.message) }
  }
  // Envia uma arma do inventário para a aba Combate (cria o ataque; idempotente).
  async function enviarProCombate(it) {
    try {
      await api(`/api/inventario/${it.id}/para-combate`, { method: 'POST' })
      await recarregarAtaques()
      setAba('Combate')
    } catch (e) { setErro(e.message) }
  }
  async function setQtd(it, delta) {
    const q = Math.max(0, (it.quantidade || 1) + delta)
    if (q === 0) return delItem(it.id)
    try { await api(`/api/inventario/${it.id}`, { method: 'PUT', body: { quantidade: q, equipado: it.equipado } }); recarregarInv() }
    catch (e) { setErro(e.message) }
  }
  async function toggleEquip(it) {
    try { await api(`/api/inventario/${it.id}`, { method: 'PUT', body: { equipado: !it.equipado } }); recarregarInv() }
    catch (e) { setErro(e.message) }
  }
  async function delItem(iid) {
    try { await api(`/api/inventario/${iid}`, { method: 'DELETE' }); recarregarInv() } catch (e) { setErro(e.message) }
  }
  async function salvarEdicao() {
    if (!editItem) return
    try {
      await api(`/api/inventario/${editItem.id}`, {
        method: 'PUT',
        body: {
          nome: editItem.nome,
          categoria: editItem.categoria || '',
          espacos: Number(editItem.espacos) || 0,
          quantidade: Number(editItem.quantidade) || 1,
          equipado: !!editItem.equipado,
          dano: editItem.dano || '',
          critico: editItem.critico || '',
          alcance: editItem.alcance || '',
          bonusDefesa: editItem.bonusDefesa === '' || editItem.bonusDefesa == null ? null : Number(editItem.bonusDefesa),
          efeito: editItem.efeito || '',
        },
      })
      setEditItem(null)
      recarregarInv()
    } catch (e) { setErro(e.message) }
  }

  // ----- habilidades (gated por trilha/nível/atributo; trilha só nível 11) -----
  const recarregarHab = () => {
    api(`/api/personagens/${id}/habilidades`).then(setHabChosen).catch(() => {})
    api(`/api/personagens/${id}/habilidades/disponiveis`).then(setHabDisp).catch(() => {})
  }
  async function addHab(codigo) {
    const cod = codigo || habSel
    if (!cod) return
    try { await api(`/api/personagens/${id}/habilidades`, { method: 'POST', body: { codigo: cod } }); setHabSel(''); recarregarHab() }
    catch (e) { setErro(e.message) }
  }
  async function delHab(codigo) {
    try { await api(`/api/personagens/${id}/habilidades/${codigo}`, { method: 'DELETE' }); recarregarHab() } catch (e) { setErro(e.message) }
  }
  async function criarPropriaHab() {
    if (!novaHab.nome.trim()) return
    try {
      await api(`/api/personagens/${id}/habilidades/custom`, {
        method: 'POST',
        body: {
          nome: novaHab.nome.trim(),
          tipo: novaHab.tipo,
          custo: Number(novaHab.custo) || 0,
          custoTipo: novaHab.custoTipo,
          efeito: novaHab.efeito,
        },
      })
      setNovaHab({ nome: '', tipo: 'PASSIVA', custo: 0, custoTipo: 'PE', efeito: '' })
      setCriarHabOpen(false)
      recarregarHab()
    } catch (e) { setErro(e.message) }
  }
  async function salvarEdicaoHab() {
    if (!editHab) return
    try {
      await api(`/api/personagens/${id}/habilidades/${editHab.codigo}`, {
        method: 'PUT',
        body: {
          nome: editHab.nome,
          tipo: editHab.tipo || '',
          custo: editHab.custo === '' || editHab.custo == null ? null : Number(editHab.custo),
          custoTipo: editHab.custoTipo || '',
          efeito: editHab.efeito || '',
        },
      })
      setEditHab(null)
      recarregarHab()
    } catch (e) { setErro(e.message) }
  }

  if (erro) return <div><p className="error">{erro}</p></div>
  if (!p) return <div className="center">Carregando…</div>

  return (
    <>
      {levelUp && (
        <div className="modal" onClick={() => setLevelUp(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>⬆️ Subiu de nível!</h2>
            {levelUp.map((g) => (
              <div key={g.nivel} className="item-card">
                <div className="t">Nível {g.nivel}</div>
                <div className="s">{g.recompensa}{g.limiteAtributo ? ` · Teto de atributo: ${g.limiteAtributo}` : ''}</div>
              </div>
            ))}
            <button style={{ marginTop: 10 }} onClick={() => setLevelUp(null)}>Fechar</button>
          </div>
        </div>
      )}

      {editStatus && (
        <div className="modal" onClick={() => setEditStatus(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 380 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Editar {editStatus.rot}</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setEditStatus(null)}>✕</button>
            </div>
            <div className="row" style={{ gap: 8, marginTop: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Atual</label>
                <input type="number" min="0" autoFocus value={editStatus.atual}
                  onChange={(e) => setEditStatus((s) => ({ ...s, atual: e.target.value }))}
                  onKeyDown={(e) => { if (e.key === 'Enter') salvarEditStatus() }} />
              </div>
              <div style={{ flex: 1 }}>
                <label>Máximo (teto)</label>
                <input type="number" min="1" value={editStatus.max}
                  onChange={(e) => setEditStatus((s) => ({ ...s, max: e.target.value }))}
                  onKeyDown={(e) => { if (e.key === 'Enter') salvarEditStatus() }} />
              </div>
            </div>
            <p className="muted" style={{ fontSize: '.78rem', marginTop: 6 }}>
              Mudar o teto fixa um valor manual. Use “Voltar ao automático” para recalcular pelas regras.
            </p>
            <div className="row" style={{ marginTop: 10, gap: 8 }}>
              <button onClick={salvarEditStatus}>Salvar</button>
              <button className="ghost" onClick={resetStatusMax}>Voltar ao automático</button>
            </div>
          </div>
        </div>
      )}

      {apagarOpen && (
        <div className="modal" onClick={() => setApagarOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 460 }}>
            <h2>Apagar ficha?</h2>
            <p className="muted">
              A ficha de <b>{p.nome}</b> e tudo nela (inventário, ataques, magias, habilidades,
              snapshots e vínculos de campanha) serão apagados. Não dá pra desfazer.
            </p>
            <label>Digite <b>Apagar</b> ou <b>remover</b> para confirmar</label>
            <input
              autoFocus
              value={apagarTxt}
              onChange={(e) => setApagarTxt(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && palavraConfere) apagarFicha() }}
              placeholder="Apagar"
            />
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button className="danger" disabled={!palavraConfere} onClick={apagarFicha}>
                Sim, apagar
              </button>
              <button className="ghost" onClick={() => setApagarOpen(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      <div className="page-head">
        <h1>{p.nome}</h1>
        <span className="count-badge">
          {p.classeNome}{p.trilhaNome ? ` · ${p.trilhaNome}` : ''} · Nv {p.nivel}
        </span>
        {campanhaAtiva && (
          <Link to={`/campanhas/${campanhaAtiva.id}`} className="tag">💬 Campanha: {campanhaAtiva.nome}</Link>
        )}
        <div className="spacer" />
        <button className="ghost mini" onClick={abrirOverlay}
          title="Abre o overlay pro OBS (retrato + dados) e copia a URL">
          📺 {overlayCopiado ? 'URL copiada!' : 'Overlay OBS'}
        </button>
        <button className="ghost mini" onClick={() => { setApagarTxt(''); setApagarOpen(true) }}>
          🗑 Apagar ficha
        </button>
      </div>
      <div className="ficha">
        {/* Coluna esquerda: identidade, atributos e status */}
        <div className="ficha-col">
          <div className="row" style={{ gap: 10 }}>
            <label className="avatar" title="Trocar foto" style={{ cursor: 'pointer', overflow: 'hidden' }}>
              {p.avatarAssetId
                ? <img src={`/api/assets/${p.avatarAssetId}/conteudo`} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                : <span className="muted" style={{ fontSize: 11 }}>+ foto</span>}
              <input type="file" accept="image/*" style={{ display: 'none' }}
                onChange={(e) => trocarFoto(e.target.files[0])} />
            </label>
            <div style={{ flex: 1 }}>
              <div className="kv"><b>Raça</b><span>{p.racaNome}</span></div>
              <div className="kv"><b>Classe</b><span>{p.classeNome}</span></div>
              {(() => {
                const trilhas = classesCat.filter((c) => c.classePaiCodigo === p.classeCodigo)
                if (!trilhas.length) {
                  return p.trilhaNome ? <div className="kv"><b>Trilha</b><span>{p.trilhaNome}</span></div> : null
                }
                return (
                  <div className="kv"><b>Trilha</b>
                    <select value={p.trilhaCodigo || ''} style={{ maxWidth: 160 }} disabled={p.nivel < 11}
                      title={p.nivel < 11 ? 'Trilha só a partir do nível 11' : undefined}
                      onChange={(e) => salvar({ trilhaCodigo: e.target.value })}>
                      <option value="">{p.nivel < 11 ? 'nível 11+' : '— nenhuma —'}</option>
                      {trilhas.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}
                    </select>
                  </div>
                )
              })()}
              {/* Multiclasse: classe e trilha secundárias */}
              <div className="kv"><b>Classe 2ª</b>
                <select value={p.classeSecundariaCodigo || ''} style={{ maxWidth: 160 }}
                  onChange={(e) => salvar({ classeSecundariaCodigo: e.target.value })}>
                  <option value="">— nenhuma —</option>
                  {classesCat.filter((c) => !c.classePaiCodigo && c.codigo !== p.classeCodigo)
                    .map((c) => <option key={c.codigo} value={c.codigo}>{c.nome}</option>)}
                </select>
              </div>
              {p.classeSecundariaCodigo && (() => {
                const ts = classesCat.filter((c) => c.classePaiCodigo === p.classeSecundariaCodigo)
                if (!ts.length) return null
                return (
                  <div className="kv"><b>Trilha 2ª</b>
                    <select value={p.trilhaSecundariaCodigo || ''} style={{ maxWidth: 160 }} disabled={p.nivel < 11}
                      title={p.nivel < 11 ? 'Trilha só a partir do nível 11' : undefined}
                      onChange={(e) => salvar({ trilhaSecundariaCodigo: e.target.value })}>
                      <option value="">{p.nivel < 11 ? 'nível 11+' : '— nenhuma —'}</option>
                      {ts.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}
                    </select>
                  </div>
                )
              })()}
            </div>
          </div>

          <div className="mandala">
            <Heptagono atributos={p.atributosFinais} max={20} />
          </div>

          {/* Editor de atributos (5 pontos distribuíveis sobre os fixos da classe) */}
          <div className="atr-edit">
            <div className="row">
              <b>Atributos</b>
              <div className="spacer" />
              <span className="tag" title="teto por atributo no nível atual">teto {tetoAtributo}</span>
              <button className="mini" onClick={salvarAtributos}>Salvar</button>
            </div>
            <div className="atr-grid">
              {ATRIBS.map(([k, sig]) => (
                <div key={k} className="atr-cell">
                  <span className="muted">{sig}</span>
                  <span className="step">
                    <button className="ghost mini" onClick={() => setAtr(k, -1)}>−</button>
                    <b className="stat">{base[k] ?? 0}</b>
                    <button className="ghost mini" onClick={() => setAtr(k, +1)}>+</button>
                  </span>
                  <span className="muted" title="final (base + fixos da classe)">= {(base[k] ?? 0) + bonusClasse(k)}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Nível e XP (level-up automático) */}
          <div className="atr-edit">
            <div className="row" style={{ gap: 8 }}>
              <label style={{ flex: 1 }}>Nível
                <input type="number" min="1" value={nivelInput} onChange={(e) => setNivelInput(e.target.value)} />
              </label>
              <label style={{ flex: 2 }}>XP
                <input type="number" min="0" value={xpInput} onChange={(e) => setXpInput(e.target.value)} />
              </label>
              <button className="mini" style={{ alignSelf: 'flex-end' }} onClick={salvarProgresso}>Salvar</button>
            </div>
            <div className="muted" style={{ marginTop: 4 }}>
              {p.xpProximoNivel != null
                ? `XP ${p.xpAtual}/${p.xpProximoNivel} para o nível ${p.nivel + 1}`
                : `Nível máximo · XP ${p.xpAtual}`}
            </div>
          </div>

          <div className="row">
            <span className="tag">Deslocamento {p.deslocamento}m</span>
            <span className="tag">Carga máx {p.cargaMaxima}</span>
          </div>

          {BARRAS.map(([rot, cls, k]) => {
            const atual = p.status[k + 'Atual']
            const max = p.status[k + 'Max']
            const pct = max > 0 ? Math.min(100, Math.round((atual / max) * 100)) : 0
            return (
              <div key={k} className="recurso">
                <div className="rec-rot">
                  {rot}
                  <button className="rec-edit" title="Editar valor e teto" onClick={() => abrirEditStatus(k, rot)}>✎</button>
                </div>
                <div className={`rec-bar ${cls}`}>
                  <span className="rec-fill" style={{ width: pct + '%' }} />
                  <button className="rec-step lg" title="-5" onClick={() => ajustar(k + 'Atual', -5)}>«</button>
                  <button className="rec-step" title="-1" onClick={() => ajustar(k + 'Atual', -1)}>‹</button>
                  <b className="rec-val" title="Editar valor e teto" style={{ cursor: 'pointer' }}
                    onClick={() => abrirEditStatus(k, rot)}>{atual}/{max}</b>
                  <button className="rec-step" title="+1" onClick={() => ajustar(k + 'Atual', +1)}>›</button>
                  <button className="rec-step lg" title="+5" onClick={() => ajustar(k + 'Atual', +5)}>»</button>
                </div>
              </div>
            )
          })}

          <div className="kv" style={{ marginTop: 12 }}>
            <b>Limites</b><span>Hab ∞ · Fei {p.limiteFeiticos} · Bên {p.limiteBencaos}</span>
          </div>
        </div>

        {/* Centro: perícias com treino */}
        <div className="ficha-col">
          <div className="row">
            <h2>Perícias</h2>
            <div className="spacer" />
            <button className="mini ghost" title="Rolar um d20" onClick={() => rolar('d20', 0)}>🎲 d20</button>
            <button className="mini" onClick={() => salvar({ pericias: treino, periciasOutros: outrosBonus, periciasCustom: outros })}>Salvar</button>
          </div>
          <table className="pericias">
            <thead><tr><th>Perícia</th><th>Atr</th><th>Treino</th><th>Outros</th><th>Teto</th></tr></thead>
            <tbody>
              {p.pericias.filter((pe) => !pe.custom).map((pe) => {
                const bonusClasse = pe.bonus || 0
                const mod = (treino[pe.codigo] ?? 0) + bonusClasse + (outrosBonus[pe.codigo] ?? 0)
                return (
                <tr key={pe.codigo} className={mod > 0 ? 'treinada' : undefined}>
                  <td>
                    <span className="per-nome">
                      <button className="d20-btn" title={`Rolar ${pe.nome} (${mod >= 0 ? '+' : ''}${mod})`}
                        onClick={() => rolar(pe.nome, mod)}>d20</button>
                      {pe.nome}
                      {bonusClasse > 0 && <span className="tag" title="bônus fixo de classe/trilha">+{bonusClasse}</span>}
                    </span>
                  </td>
                  <td className="muted">{pe.sigla}</td>
                  <td>
                    <span className="step">
                      <button className="ghost mini" onClick={() => setTr(pe.codigo, -1, pe.cap)}>−</button>
                      <b className="stat">{treino[pe.codigo] ?? 0}</b>
                      <button className="ghost mini" onClick={() => setTr(pe.codigo, +1, pe.cap)}>+</button>
                    </span>
                  </td>
                  <td>
                    <span className="step">
                      <button className="ghost mini" onClick={() => setOutro(pe.codigo, -1)}>−</button>
                      <b className="stat">{outrosBonus[pe.codigo] ?? 0}</b>
                      <button className="ghost mini" onClick={() => setOutro(pe.codigo, +1)}>+</button>
                    </span>
                  </td>
                  <td className="muted stat">{pe.cap}</td>
                </tr>
                )
              })}
              {outros.map((o, idx) => {
                const cap = 2 * ((p.atributosFinais[o.atributo.toLowerCase()]) || 0)
                return (
                  <tr key={'outro-' + idx} className={o.treino > 0 ? 'treinada' : undefined}>
                    <td>
                      <span className="per-nome">
                        <button className="d20-btn" title={`Rolar ${o.nome}`}
                          onClick={() => rolar(o.nome, o.treino)}>d20</button>
                        {o.nome} <span className="tag">Extra</span>
                      </span>
                    </td>
                    <td className="muted">{siglaDe(o.atributo)}</td>
                    <td>
                      <span className="step">
                        <button className="ghost mini" onClick={() => setTrCustom(idx, -1, cap)}>−</button>
                        <b className="stat">{o.treino}</b>
                        <button className="ghost mini" onClick={() => setTrCustom(idx, +1, cap)}>+</button>
                      </span>
                    </td>
                    <td className="muted" style={{ textAlign: 'center' }}>—</td>
                    <td className="muted stat">{cap} <button className="ghost mini" onClick={() => delOutro(idx)}>✕</button></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          <div className="add-form">
            <input placeholder="Perícia de item" value={novaPericia.nome}
              onChange={(e) => setNovaPericia((s) => ({ ...s, nome: e.target.value }))} />
            <select value={novaPericia.atributo} onChange={(e) => setNovaPericia((s) => ({ ...s, atributo: e.target.value }))}>
              {ATRIBS.map(([k, sig]) => <option key={k} value={k.toUpperCase()}>{sig}</option>)}
            </select>
            <button className="mini" onClick={addOutro}>+ Extra</button>
          </div>
        </div>

        {/* Direita: abas */}
        <div className="ficha-col">
          <div className="abas">
            {ABAS.map((x) => (
              <button key={x} className={aba === x ? 'ativo' : undefined} onClick={() => setAba(x)}>{x}</button>
            ))}
          </div>

          {aba === 'Combate' && (
            <div>
              <div className="cris-list">
                {ataques.map((a) => (
                  <AtaqueRow key={a.id} a={a} onEdit={setEditAtaque} onDelete={delAtaque} />
                ))}
              </div>
              {!ataques.length && <div className="muted">Nenhum ataque cadastrado.</div>}
              <div className="add-form">
                <input placeholder="Nome" value={novoAtaque.nome}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, nome: e.target.value }))} />
                <input placeholder="Dano (1d8)" value={novoAtaque.dano}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, dano: e.target.value }))} />
                <input placeholder="Crít (x2, x3…)" value={novoAtaque.critico}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, critico: e.target.value }))} />
                <input placeholder="Alcance" value={novoAtaque.alcance}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, alcance: e.target.value }))} />
                <button className="mini" onClick={addAtaque}>+ Ataque</button>
              </div>
              <div className="muted" style={{ marginTop: 8 }}>Para rolar dados em mesa, abra a campanha.</div>
            </div>
          )}

          {aba === 'Habilidades' && (
            <div>
              <div className="muted" style={{ marginBottom: 6 }}>{habChosen.length} habilidade{habChosen.length === 1 ? '' : 's'} (sem limite)</div>
              <div className="cris-list">
                {habChosen.map((h) => (
                  <HabRow key={h.codigo} h={h} onEdit={setEditHab} onDelete={delHab} />
                ))}
              </div>
              {!habChosen.length && <div className="muted">Nenhuma habilidade escolhida.</div>}
              <div className="row" style={{ gap: 8, marginTop: 4 }}>
                <button className="mini" onClick={() => { setHabBusca(''); setModalHab(true) }}>+ Adicionar Habilidade</button>
                <button className="mini ghost" onClick={() => setCriarHabOpen(true)}>✎ Criar própria</button>
              </div>
              {!habDisp.length && (
                <div className="muted" style={{ marginTop: 6 }}>
                  Nada liberado no nível/atributo atuais (a trilha só conta a partir do nível 11).
                </div>
              )}
            </div>
          )}

          {aba === 'Magias' && (
            <div>
              <div className="muted" style={{ marginBottom: 8 }}>
                Construa feitiços pelas regras em <b>Livros → Feitiços</b> (círculo, alcance, poder, duração).
              </div>
              <div className="cris-list">
                {feiticos.map((f) => (
                  <FeiticoRow key={f.id} f={f} onEdit={setEditFeitico} onDelete={delFeitico} />
                ))}
              </div>
              {!feiticos.length && <div className="muted">Nenhum feitiço cadastrado.</div>}
              <div className="add-form" style={{ alignItems: 'flex-end' }}>
                <div style={{ flex: '1 1 130px' }}>
                  <label>Nome</label>
                  <input placeholder="Nome do feitiço" value={novoFeitico.nome}
                    onChange={(e) => setNovoFeitico((s) => ({ ...s, nome: e.target.value }))} />
                </div>
                <div style={{ width: 80 }}>
                  <label>Círculo</label>
                  <input type="number" min="1" max="5" value={novoFeitico.circulo}
                    onChange={(e) => setNovoFeitico((s) => ({ ...s, circulo: e.target.value }))} />
                </div>
                <div style={{ width: 80 }}>
                  <label>Custo PM</label>
                  <input type="number" min="0" value={novoFeitico.custoPm}
                    onChange={(e) => setNovoFeitico((s) => ({ ...s, custoPm: e.target.value }))} />
                </div>
                <div style={{ flex: '1 1 90px' }}>
                  <label>Alcance</label>
                  <input value={novoFeitico.alcance}
                    onChange={(e) => setNovoFeitico((s) => ({ ...s, alcance: e.target.value }))} />
                </div>
                <div style={{ flex: '1 1 120px' }}>
                  <label>Efeito</label>
                  <input value={novoFeitico.efeito}
                    onChange={(e) => setNovoFeitico((s) => ({ ...s, efeito: e.target.value }))} />
                </div>
                <button className="mini" onClick={addFeitico}>+ Feitiço</button>
              </div>
            </div>
          )}

          {aba === 'Inventário' && (
            <div>
              <div className="bar-label">Carga {fmtEsp(p.cargaAtual)}/{p.cargaMaxima} espaços</div>
              <div className={`bar ${p.cargaAtual > p.cargaMaxima ? 'energia' : 'vida'}`} style={{ marginBottom: 6 }}>
                <span style={{ width: Math.min(100, p.cargaMaxima ? Math.round((p.cargaAtual / p.cargaMaxima) * 100) : 0) + '%' }} />
                <b>{fmtEsp(p.cargaAtual)}/{p.cargaMaxima}</b>
              </div>
              {p.cargaAtual > p.cargaMaxima && (
                <div className="error" style={{ marginBottom: 8 }}>
                  Sobrecarregado!{Math.floor(p.cargaAtual - p.cargaMaxima) >= 1
                    ? ` −${Math.floor(p.cargaAtual - p.cargaMaxima)} de Agilidade (1 por ponto inteiro acima da carga máxima).`
                    : ' (a partir de 1 ponto inteiro acima, perde Agilidade).'}
                </div>
              )}

              <div className="cris-list">
                {inventario.map((it) => (
                  <ItemInvRow key={it.id} it={it} onQtd={setQtd} onEquip={toggleEquip}
                    onDelete={delItem} onEdit={setEditItem} onCombate={enviarProCombate} />
                ))}
              </div>
              {!inventario.length && <div className="muted">Inventário vazio.</div>}

              <div className="add-form">
                <select value={itemCat} onChange={(e) => setItemCat(e.target.value)} style={{ flex: '1 1 160px' }}>
                  <option value="">— item do catálogo (T20) —</option>
                  {itens.map((i) => (
                    <option key={i.codigo} value={i.codigo}>{i.nome}{i.dano ? ` (${i.dano})` : ''}</option>
                  ))}
                </select>
                <button className="mini" onClick={addCatalogo}>+ Adicionar</button>
              </div>
              <div className="add-form">
                <input placeholder="Item próprio" value={novoItem.nome}
                  onChange={(e) => setNovoItem((s) => ({ ...s, nome: e.target.value }))} />
                <input placeholder="Dano (1d8)" style={{ maxWidth: 100 }} value={novoItem.dano || ''}
                  onChange={(e) => setNovoItem((s) => ({ ...s, dano: e.target.value }))} />
                <input placeholder="Crít (x2, x3…)" style={{ maxWidth: 110 }} value={novoItem.critico || ''}
                  onChange={(e) => setNovoItem((s) => ({ ...s, critico: e.target.value }))} />
                <input type="number" min="0" step="0.5" placeholder="Espaços" style={{ maxWidth: 90 }} value={novoItem.espacos}
                  onChange={(e) => setNovoItem((s) => ({ ...s, espacos: e.target.value }))} />
                <button className="mini" onClick={addItemProprio}>+ Próprio</button>
              </div>
              <div className="muted" style={{ fontSize: '.76rem', marginTop: 2 }}>
                Item com <b>dano</b> vira arma e cria um ataque no Combate automaticamente. Já tem armas no inventário? Use o botão <b>⚔ Combate</b> em cada uma.
              </div>
            </div>
          )}

          {aba === 'Descrição' && (
            <div>
              {CAMPOS_DESC.map(([k, rot]) => (
                <div key={k}>
                  <label>{rot}</label>
                  <textarea value={desc[k]} onChange={(e) => setDesc((dd) => ({ ...dd, [k]: e.target.value }))} />
                </div>
              ))}
              <button style={{ marginTop: 10 }} onClick={() => salvar(desc)}>Salvar descrição</button>
            </div>
          )}
        </div>
      </div>

      {editItem && (
        <div className="modal" onClick={() => setEditItem(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Editar item</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setEditItem(null)}>✕</button>
            </div>
            <label>Nome</label>
            <input value={editItem.nome || ''} onChange={(e) => setEditItem((s) => ({ ...s, nome: e.target.value }))} />
            <div className="row" style={{ gap: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Categoria</label>
                <input value={editItem.categoria || ''}
                  onChange={(e) => setEditItem((s) => ({ ...s, categoria: e.target.value }))} />
              </div>
              <div style={{ width: 90 }}>
                <label>Espaços</label>
                <input type="number" min="0" step="0.5" value={editItem.espacos ?? 0}
                  onChange={(e) => setEditItem((s) => ({ ...s, espacos: e.target.value }))} />
              </div>
            </div>
            <div className="row" style={{ gap: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Dano</label>
                <input placeholder="1d8" value={editItem.dano || ''}
                  onChange={(e) => setEditItem((s) => ({ ...s, dano: e.target.value }))} />
              </div>
              <div style={{ width: 90 }}>
                <label>Crítico</label>
                <input placeholder="x2, x3…" value={editItem.critico || ''}
                  onChange={(e) => setEditItem((s) => ({ ...s, critico: e.target.value }))} />
              </div>
              <div style={{ width: 100 }}>
                <label>Defesa +</label>
                <input type="number" value={editItem.bonusDefesa ?? ''}
                  onChange={(e) => setEditItem((s) => ({ ...s, bonusDefesa: e.target.value }))} />
              </div>
            </div>
            <label>Alcance</label>
            <input value={editItem.alcance || ''} onChange={(e) => setEditItem((s) => ({ ...s, alcance: e.target.value }))} />
            <label>Efeito / Descrição</label>
            <textarea value={editItem.efeito || ''} onChange={(e) => setEditItem((s) => ({ ...s, efeito: e.target.value }))} />
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button onClick={salvarEdicao}>Salvar</button>
              <button className="ghost" onClick={() => setEditItem(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {editAtaque && (
        <div className="modal" onClick={() => setEditAtaque(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Editar ataque</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setEditAtaque(null)}>✕</button>
            </div>
            <label>Nome</label>
            <input value={editAtaque.nome || ''} onChange={(e) => setEditAtaque((s) => ({ ...s, nome: e.target.value }))} />
            <div className="row" style={{ gap: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Dano</label>
                <input placeholder="1d8" value={editAtaque.dano || ''}
                  onChange={(e) => setEditAtaque((s) => ({ ...s, dano: e.target.value }))} />
              </div>
              <div style={{ width: 110 }}>
                <label>Crítico</label>
                <input placeholder="x2, x3…" value={editAtaque.critico || ''}
                  onChange={(e) => setEditAtaque((s) => ({ ...s, critico: e.target.value }))} />
              </div>
            </div>
            <div className="row" style={{ gap: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Alcance</label>
                <input value={editAtaque.alcance || ''}
                  onChange={(e) => setEditAtaque((s) => ({ ...s, alcance: e.target.value }))} />
              </div>
              <div style={{ flex: 1 }}>
                <label>Perícia</label>
                <input value={editAtaque.pericia || ''}
                  onChange={(e) => setEditAtaque((s) => ({ ...s, pericia: e.target.value }))} />
              </div>
            </div>
            <label>Efeito / Descrição</label>
            <textarea value={editAtaque.efeito || ''} onChange={(e) => setEditAtaque((s) => ({ ...s, efeito: e.target.value }))} />
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button onClick={salvarEdicaoAtaque}>Salvar</button>
              <button className="ghost" onClick={() => setEditAtaque(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {editFeitico && (
        <div className="modal" onClick={() => setEditFeitico(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Editar feitiço</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setEditFeitico(null)}>✕</button>
            </div>
            <label>Nome</label>
            <input value={editFeitico.nome || ''} onChange={(e) => setEditFeitico((s) => ({ ...s, nome: e.target.value }))} />
            <div className="row" style={{ gap: 8 }}>
              <div style={{ width: 110 }}>
                <label>Círculo</label>
                <input type="number" min="1" max="5" value={editFeitico.circulo ?? ''}
                  onChange={(e) => setEditFeitico((s) => ({ ...s, circulo: e.target.value }))} />
              </div>
              <div style={{ width: 110 }}>
                <label>Custo PM</label>
                <input type="number" min="0" value={editFeitico.custoPm ?? ''}
                  onChange={(e) => setEditFeitico((s) => ({ ...s, custoPm: e.target.value }))} />
              </div>
              <div style={{ flex: 1 }}>
                <label>Alcance</label>
                <input value={editFeitico.alcance || ''}
                  onChange={(e) => setEditFeitico((s) => ({ ...s, alcance: e.target.value }))} />
              </div>
            </div>
            <label>Duração</label>
            <input value={editFeitico.duracao || ''} onChange={(e) => setEditFeitico((s) => ({ ...s, duracao: e.target.value }))} />
            <label>Efeito / Descrição</label>
            <textarea value={editFeitico.efeito || ''} onChange={(e) => setEditFeitico((s) => ({ ...s, efeito: e.target.value }))} />
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button onClick={salvarEdicaoFeitico}>Salvar</button>
              <button className="ghost" onClick={() => setEditFeitico(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {editHab && (
        <div className="modal" onClick={() => setEditHab(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Editar habilidade</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setEditHab(null)}>✕</button>
            </div>
            <label>Nome</label>
            <input value={editHab.nome || ''} onChange={(e) => setEditHab((s) => ({ ...s, nome: e.target.value }))} />
            <div className="row" style={{ gap: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Tipo</label>
                <select value={editHab.tipo || ''} onChange={(e) => setEditHab((s) => ({ ...s, tipo: e.target.value }))}>
                  <option value="">—</option>
                  <option value="ATIVA">ATIVA</option>
                  <option value="PASSIVA">PASSIVA</option>
                </select>
              </div>
              <div style={{ width: 90 }}>
                <label>Custo</label>
                <input type="number" min="0" value={editHab.custo ?? ''}
                  onChange={(e) => setEditHab((s) => ({ ...s, custo: e.target.value }))} />
              </div>
              <div style={{ width: 100 }}>
                <label>Tipo custo</label>
                <select value={editHab.custoTipo || ''} onChange={(e) => setEditHab((s) => ({ ...s, custoTipo: e.target.value }))}>
                  <option value="">—</option>
                  <option value="PE">PE</option>
                  <option value="PM">PM</option>
                </select>
              </div>
            </div>
            <label>Efeito / Descrição</label>
            <textarea value={editHab.efeito || ''} onChange={(e) => setEditHab((s) => ({ ...s, efeito: e.target.value }))} />
            <p className="muted" style={{ fontSize: '.78rem', marginTop: 2 }}>
              A edição vale só para este personagem. Deixe um campo vazio para usar o valor padrão do catálogo.
            </p>
            <div className="row" style={{ marginTop: 10, gap: 8 }}>
              <button onClick={salvarEdicaoHab}>Salvar</button>
              <button className="ghost" onClick={() => setEditHab(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {modalHab && (
        <div className="modal" onClick={() => setModalHab(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 560 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Adicionar Habilidade</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setModalHab(false)}>✕</button>
            </div>
            <div className="search-wrap" style={{ margin: '12px 0' }}>
              <span className="ic">🔍</span>
              <input placeholder="Filtrar habilidades" value={habBusca}
                onChange={(e) => setHabBusca(e.target.value)} autoFocus />
            </div>
            <div className="lista-vert" style={{ maxHeight: '52vh', overflow: 'auto' }}>
              {habDisp
                .filter((h) => (h.nome + ' ' + (h.classeCodigo || '')).toLowerCase().includes(habBusca.toLowerCase()))
                .map((h) => (
                  <div key={h.codigo} className="hab-opt">
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <b>{h.nome}</b>
                      <div className="row" style={{ gap: 6, marginTop: 3 }}>
                        {h.classeCodigo && h.classeCodigo !== 'GERAL' && <span className="tag">{fmtClasses(h.classeCodigo)}</span>}
                        {h.nivelMinimo > 1 && <span className="tag">Nv {h.nivelMinimo}</span>}
                        {h.tipo && <span className="tag">{h.tipo}</span>}
                      </div>
                      {h.efeito && <div className="muted" style={{ fontSize: '.78rem', marginTop: 3 }}>{h.efeito}</div>}
                    </div>
                    <button className="mini" title="Adicionar" onClick={() => addHab(h.codigo)}>+</button>
                  </div>
                ))}
              {!habDisp.length && <div className="muted">Nenhuma habilidade disponível no nível/atributo atuais.</div>}
            </div>
          </div>
        </div>
      )}

      {criarHabOpen && (
        <div className="modal" onClick={() => setCriarHabOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="row">
              <h2 style={{ margin: 0 }}>Criar habilidade própria</h2>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => setCriarHabOpen(false)}>✕</button>
            </div>
            <label>Nome</label>
            <input autoFocus value={novaHab.nome} onChange={(e) => setNovaHab((s) => ({ ...s, nome: e.target.value }))} />
            <div className="row" style={{ gap: 8 }}>
              <div style={{ flex: 1 }}>
                <label>Tipo</label>
                <select value={novaHab.tipo} onChange={(e) => setNovaHab((s) => ({ ...s, tipo: e.target.value }))}>
                  <option value="PASSIVA">PASSIVA</option>
                  <option value="ATIVA">ATIVA</option>
                </select>
              </div>
              <div style={{ width: 90 }}>
                <label>Custo</label>
                <input type="number" min="0" value={novaHab.custo}
                  onChange={(e) => setNovaHab((s) => ({ ...s, custo: e.target.value }))} />
              </div>
              <div style={{ width: 100 }}>
                <label>Tipo custo</label>
                <select value={novaHab.custoTipo} onChange={(e) => setNovaHab((s) => ({ ...s, custoTipo: e.target.value }))}>
                  <option value="PE">PE</option>
                  <option value="PM">PM</option>
                </select>
              </div>
            </div>
            <label>Efeito / Descrição</label>
            <textarea value={novaHab.efeito} onChange={(e) => setNovaHab((s) => ({ ...s, efeito: e.target.value }))} />
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button onClick={criarPropriaHab}>Criar</button>
              <button className="ghost" onClick={() => setCriarHabOpen(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {rolagem && (
        <div className={`roll-toast ${rolagem.crit ? 'crit' : rolagem.fumble ? 'fumble' : ''}`}>
          <div className="muted">{rolagem.rotulo}</div>
          <div className="rt-total">
            {rolagem.total}{rolagem.crit ? ' ✦' : ''}{rolagem.fumble ? ' ✗' : ''}
          </div>
          <div className="muted" style={{ fontSize: '.75rem' }}>
            d20 ({rolagem.d}){rolagem.mod ? ` + ${rolagem.mod}` : ''}
            {rolagem.crit ? ' · CRÍTICO!' : ''}{rolagem.fumble ? ' · FALHA!' : ''}
          </div>
        </div>
      )}

      {campanhaAtiva && (
        <ResultadosPanel
          rolagens={rolagens}
          onRolar={rolarPainel}
          ehMestre={!!user && campanhaAtiva.mestreId === user.id}
        />
      )}
    </>
  )
}
