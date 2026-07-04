import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'

const ATRS = ['forca', 'constituicao', 'destreza', 'agilidade', 'inteligencia', 'sabedoria', 'carisma']

export default function Personagens() {
  const [orgId, setOrgId] = useState(null)
  const [lista, setLista] = useState([])
  const [racas, setRacas] = useState([])
  const [classes, setClasses] = useState([])
  const [busca, setBusca] = useState('')
  const [erro, setErro] = useState(null)
  const [nivel1Cap, setNivel1Cap] = useState(99)
  const [progressao, setProgressao] = useState([])
  const [form, setForm] = useState({
    nome: '', racaCodigo: '', classeCodigo: '', trilhaCodigo: '', nivel: 1,
    atributos: Object.fromEntries(ATRS.map((a) => [a, 0])),
  })

  const carregar = (oid) => api(`/api/organizacoes/${oid}/personagens`).then(setLista)

  useEffect(() => {
    (async () => {
      const oid = await obterOrgId()
      setOrgId(oid)
      carregar(oid)
      const rs = await api('/api/sistemas/asus/racas')
      const cs = await api('/api/sistemas/asus/classes')
      setRacas(rs)
      setClasses(cs)
      const prog = await api('/api/sistemas/asus/progressao').catch(() => [])
      setProgressao(prog || [])
      const n1 = (prog || []).find((x) => x.nivel === 1)
      if (n1 && n1.limiteAtributo > 0) setNivel1Cap(n1.limiteAtributo)
      setForm((f) => ({
        ...f,
        racaCodigo: rs[0]?.codigo || '',
        classeCodigo: cs.find((c) => !c.classePaiCodigo)?.codigo || '',
      }))
    })().catch((e) => setErro(e.message))
  }, [])

  const classesBase = classes.filter((c) => !c.classePaiCodigo)
  const trilhas = classes.filter((c) => c.classePaiCodigo === form.classeCodigo)
  const pontos = ATRS.reduce((s, a) => s + (Number(form.atributos[a]) || 0), 0)

  // Bônus fixo de atributo da classe (+ trilha selecionada).
  function bonusAtributos() {
    const acc = {}
    for (const cod of [form.classeCodigo, form.trilhaCodigo]) {
      const c = classes.find((x) => x.codigo === cod)
      if (c && c.jsonBonus) {
        try {
          const atrs = JSON.parse(c.jsonBonus).atributos || {}
          for (const [k, v] of Object.entries(atrs)) acc[k] = (acc[k] || 0) + Number(v)
        } catch { /* ignora json invalido */ }
      }
    }
    return acc
  }
  const bonusMap = bonusAtributos()

  // Teto de atributo do nível escolhido (maior limite entre os níveis <= nível atual).
  const capNivel = (() => {
    const lvl = Number(form.nivel) || 1
    let cap = nivel1Cap
    for (const x of progressao) if (x.nivel <= lvl && x.limiteAtributo > 0) cap = Math.max(cap, x.limiteAtributo)
    return cap
  })()

  // Pontos de atributo: base 5 + 2 por nível "com pontos" (a cada 5 níveis vira bônus de classe+raça).
  const nivelSel = Number(form.nivel) || 1
  const niveisComPontos = Math.max(0, (nivelSel - 1) - Math.floor(nivelSel / 5))
  const maxPontos = 5 + 2 * niveisComPontos
  const NIVEL_TRILHA = 11
  const podeTrilha = nivelSel >= NIVEL_TRILHA

  const setAtr = (a, delta) =>
    setForm((f) => {
      const cap = capNivel - (bonusMap[a] || 0) // teto do nível escolhido menos o fixo da classe
      const atual = Number(f.atributos[a]) || 0
      const novo = Math.max(0, Math.min(cap, atual + delta))
      const soma = pontos - atual + novo
      if (delta > 0 && soma > maxPontos) return f
      return { ...f, atributos: { ...f.atributos, [a]: novo } }
    })

  async function criar(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api(`/api/organizacoes/${orgId}/personagens`, {
        method: 'POST',
        body: {
          nome: form.nome,
          racaCodigo: form.racaCodigo,
          classeCodigo: form.classeCodigo,
          trilhaCodigo: podeTrilha ? (form.trilhaCodigo || null) : null,
          nivel: Number(form.nivel) || 1,
          atributosBase: form.atributos,
        },
      })
      setForm((f) => ({ ...f, nome: '' }))
      carregar(orgId)
    } catch (ex) {
      setErro(ex.message)
    }
  }

  const filtrados = lista.filter((p) => p.nome.toLowerCase().includes(busca.toLowerCase()))

  return (
    <>
      <div className="page-head">
        <h1>Personagens</h1>
        <span className="count-badge"><b>{lista.length}</b>/99</span>
      </div>
      {erro && <p className="error">{erro}</p>}
      <div className="search-wrap">
        <span className="ic">🔍</span>
        <input placeholder="Buscar" value={busca} onChange={(e) => setBusca(e.target.value)} />
      </div>
      <div className="grid">
        {filtrados.map((p) => (
          <Link key={p.id} to={`/personagens/${p.id}`} className="entity-card">
            <div
              className="av"
              style={p.avatarAssetId ? { backgroundImage: `url(/api/assets/${p.avatarAssetId}/conteudo)` } : undefined}
            >
              {!p.avatarAssetId && (p.nome || '?').charAt(0).toUpperCase()}
            </div>
            <div className="body">
              <div className="name">
                {p.nome}<span className="tag gold mini-lvl">Nv {p.nivel}</span>
              </div>
              <div className="sub">
                {p.racaNome} · {p.classeNome}{p.trilhaNome ? ` · ${p.trilhaNome}` : ''}
              </div>
              <div className="foot">
                <span className="tag">PV {p.status.pvMax}</span>
                <span className="tag">PM {p.status.pmMax}</span>
                <span className="tag">PE {p.status.peMax}</span>
              </div>
            </div>
          </Link>
        ))}
        {filtrados.length === 0 && <p className="muted">Nenhum personagem encontrado.</p>}
      </div>

      <div className="card">
        <h2>Novo personagem</h2>
        <form onSubmit={criar}>
          <div className="row">
            <div style={{ flex: 1, minWidth: 160 }}>
              <label>Nome</label>
              <input value={form.nome} onChange={(e) => setForm((f) => ({ ...f, nome: e.target.value }))} required />
            </div>
            <div>
              <label>Raça</label>
              <select value={form.racaCodigo} onChange={(e) => setForm((f) => ({ ...f, racaCodigo: e.target.value }))}>
                {racas.map((r) => <option key={r.codigo} value={r.codigo}>{r.nome}</option>)}
              </select>
            </div>
            <div>
              <label>Classe</label>
              <select
                value={form.classeCodigo}
                onChange={(e) => setForm((f) => ({ ...f, classeCodigo: e.target.value, trilhaCodigo: '' }))}
              >
                {classesBase.map((c) => <option key={c.codigo} value={c.codigo}>{c.nome}</option>)}
              </select>
            </div>
            <div>
              <label>Trilha</label>
              <select value={podeTrilha ? form.trilhaCodigo : ''} disabled={!podeTrilha}
                title={podeTrilha ? undefined : `Trilha só a partir do nível ${NIVEL_TRILHA}`}
                onChange={(e) => setForm((f) => ({ ...f, trilhaCodigo: e.target.value }))}>
                <option value="">{podeTrilha ? '(nenhuma)' : `nível ${NIVEL_TRILHA}+`}</option>
                {podeTrilha && trilhas.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}
              </select>
            </div>
            <div style={{ maxWidth: 90 }}>
              <label>Nível</label>
              <input type="number" min="1" value={form.nivel}
                onChange={(e) => setForm((f) => ({ ...f, nivel: e.target.value }))} />
            </div>
          </div>
          <label style={{ marginTop: 10 }}>
            Atributos — {pontos}/{maxPontos} pontos distribuíveis (nível {Number(form.nivel) || 1}) · teto {capNivel}/atributo (os fixos da classe entram automaticamente)
          </label>
          {!podeTrilha && (
            <p className="muted" style={{ fontSize: '.8rem', marginTop: 2 }}>
              A Trilha (subclasse) só pode ser escolhida a partir do nível {NIVEL_TRILHA}.
            </p>
          )}
          <div className="row" style={{ gap: 12, flexWrap: 'wrap' }}>
            {ATRS.map((a) => (
              <div key={a} style={{ textAlign: 'center' }}>
                <label style={{ textTransform: 'capitalize' }}>{a.slice(0, 3)}</label>
                <span className="step">
                  <button type="button" className="ghost mini" onClick={() => setAtr(a, -1)}>−</button>
                  <b className="stat">{form.atributos[a]}</b>
                  <button type="button" className="ghost mini" onClick={() => setAtr(a, +1)}>+</button>
                </span>
                <div className="muted" style={{ fontSize: '.7rem' }} title="final (base + fixos da classe)">
                  = {form.atributos[a] + (bonusMap[a] || 0)}
                </div>
              </div>
            ))}
          </div>
          <button style={{ marginTop: 12 }}>Criar personagem</button>
        </form>
      </div>
    </>
  )
}
