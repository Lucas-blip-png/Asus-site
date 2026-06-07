import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'

const inf = (n) => (n >= 2147483647 ? '∞' : n)

export default function Organizacao() {
  const { orgId } = useParams()
  const [org, setOrg] = useState(null)
  const [racas, setRacas] = useState([])
  const [classes, setClasses] = useState([])
  const [personagens, setPersonagens] = useState([])
  const [campanhas, setCampanhas] = useState([])
  const [limites, setLimites] = useState(null)
  const [erro, setErro] = useState(null)

  const [pnome, setPnome] = useState('')
  const [raca, setRaca] = useState('')
  const [classe, setClasse] = useState('')
  const [cnome, setCnome] = useState('')

  function carregar() {
    api(`/api/organizacoes/${orgId}`).then(setOrg).catch((e) => setErro(e.message))
    api(`/api/organizacoes/${orgId}/personagens`).then(setPersonagens)
    api(`/api/organizacoes/${orgId}/campanhas`).then(setCampanhas)
    api(`/api/organizacoes/${orgId}/limites`).then(setLimites)
  }

  useEffect(() => {
    carregar()
    api('/api/sistemas/asus/racas').then((rs) => {
      setRacas(rs)
      if (rs[0]) setRaca(rs[0].codigo)
    })
    api('/api/sistemas/asus/classes').then((cs) => {
      setClasses(cs)
      if (cs[0]) setClasse(cs[0].codigo)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgId])

  async function criarPersonagem(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api(`/api/organizacoes/${orgId}/personagens`, {
        method: 'POST',
        body: {
          nome: pnome,
          racaCodigo: raca,
          classeCodigo: classe,
          nivel: 1,
          atributosBase: { forca: 2, agilidade: 2, vigor: 2, intelecto: 2, presenca: 2 },
        },
      })
      setPnome('')
      carregar()
    } catch (ex) {
      setErro(ex.message)
    }
  }

  async function criarCampanha(e) {
    e.preventDefault()
    setErro(null)
    try {
      await api(`/api/organizacoes/${orgId}/campanhas`, { method: 'POST', body: { nome: cnome } })
      setCnome('')
      carregar()
    } catch (ex) {
      setErro(ex.message)
    }
  }

  if (!org) return <div className="center">Carregando…</div>
  return (
    <>
      <div className="row">
        <h1>{org.nome}</h1>
        <span className="tag">{org.plano}</span>
      </div>
      {erro && <p className="error">{erro}</p>}

      <div className="card">
        <h2>
          Personagens{' '}
          {limites && (
            <span className="muted">
              ({personagens.length}/{inf(limites.maxPersonagens)})
            </span>
          )}
        </h2>
        <div className="grid">
          {personagens.map((p) => (
            <Link key={p.id} to={`/personagens/${p.id}`} className="card">
              <b>{p.nome}</b>
              <div className="muted">
                {p.racaNome} · {p.classeNome} · Nv {p.nivel}
              </div>
              <div className="stat">
                PV {p.status.pvMax} · PM {p.status.pmMax} · PE {p.status.peMax}
              </div>
            </Link>
          ))}
        </div>
        <form onSubmit={criarPersonagem} className="row" style={{ marginTop: 10 }}>
          <div style={{ flex: 1 }}>
            <label>Nome</label>
            <input value={pnome} onChange={(e) => setPnome(e.target.value)} required />
          </div>
          <div>
            <label>Raça</label>
            <select value={raca} onChange={(e) => setRaca(e.target.value)}>
              {racas.map((r) => (
                <option key={r.codigo} value={r.codigo}>
                  {r.nome}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label>Classe</label>
            <select value={classe} onChange={(e) => setClasse(e.target.value)}>
              {classes.map((c) => (
                <option key={c.codigo} value={c.codigo}>
                  {c.nome}
                </option>
              ))}
            </select>
          </div>
          <button style={{ alignSelf: 'end' }}>Criar</button>
        </form>
      </div>

      <div className="card">
        <h2>
          Campanhas{' '}
          {limites && (
            <span className="muted">
              ({campanhas.length}/{inf(limites.maxCampanhas)})
            </span>
          )}
        </h2>
        <div className="grid">
          {campanhas.map((c) => (
            <Link key={c.id} to={`/campanhas/${c.id}`} className="card">
              <b>{c.nome}</b>
              <div className="muted">{c.descricao}</div>
            </Link>
          ))}
        </div>
        <form onSubmit={criarCampanha} className="row" style={{ marginTop: 10 }}>
          <div style={{ flex: 1 }}>
            <label>Nome da campanha</label>
            <input value={cnome} onChange={(e) => setCnome(e.target.value)} required />
          </div>
          <button style={{ alignSelf: 'end' }}>Criar</button>
        </form>
      </div>
    </>
  )
}
