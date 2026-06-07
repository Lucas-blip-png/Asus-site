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
  const [form, setForm] = useState({
    nome: '', racaCodigo: '', classeCodigo: '', trilhaCodigo: '',
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

  const setAtr = (a, delta) =>
    setForm((f) => {
      const atual = Number(f.atributos[a]) || 0
      const novo = Math.max(0, atual + delta)
      const soma = pontos - atual + novo
      if (delta > 0 && soma > 5) return f
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
          trilhaCodigo: form.trilhaCodigo || null,
          nivel: 0,
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
      <div className="row">
        <h1>Personagens</h1>
        <span className="muted">{lista.length}/99</span>
      </div>
      {erro && <p className="error">{erro}</p>}
      <input
        placeholder="🔍 Buscar"
        value={busca}
        onChange={(e) => setBusca(e.target.value)}
        style={{ maxWidth: 420, marginBottom: 16 }}
      />
      <div className="grid">
        {filtrados.map((p) => (
          <Link key={p.id} to={`/personagens/${p.id}`} className="card">
            <b>{p.nome}</b>
            <div className="muted">
              {p.racaNome} · {p.classeNome}{p.trilhaNome ? ` · ${p.trilhaNome}` : ''} · Nv {p.nivel}
            </div>
            <div className="row" style={{ marginTop: 8 }}>
              <span className="tag">PV {p.status.pvMax}</span>
              <span className="tag">PM {p.status.pmMax}</span>
              <span className="tag">PE {p.status.peMax}</span>
            </div>
          </Link>
        ))}
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
              <select value={form.trilhaCodigo} onChange={(e) => setForm((f) => ({ ...f, trilhaCodigo: e.target.value }))}>
                <option value="">(nenhuma)</option>
                {trilhas.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}
              </select>
            </div>
          </div>
          <label style={{ marginTop: 10 }}>
            Atributos — {pontos}/5 pontos distribuíveis (os fixos da classe entram automaticamente)
          </label>
          <div className="row" style={{ gap: 12, flexWrap: 'wrap' }}>
            {ATRS.map((a) => (
              <div key={a} style={{ textAlign: 'center' }}>
                <label style={{ textTransform: 'capitalize' }}>{a.slice(0, 3)}</label>
                <span className="step">
                  <button type="button" className="ghost mini" onClick={() => setAtr(a, -1)}>−</button>
                  <b className="stat">{form.atributos[a]}</b>
                  <button type="button" className="ghost mini" onClick={() => setAtr(a, +1)}>+</button>
                </span>
              </div>
            ))}
          </div>
          <button style={{ marginTop: 12 }}>Criar personagem</button>
        </form>
      </div>
    </>
  )
}
