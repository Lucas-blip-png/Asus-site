import { useEffect, useState } from 'react'
import { api } from '../api.js'

const ABAS = ['Atributos', 'Classes', 'Perícias', 'Itens', 'Progressão', 'Feitiços']

export default function Livros() {
  const [aba, setAba] = useState('Atributos')
  const [d, setD] = useState({})
  const [erro, setErro] = useState(null)

  useEffect(() => {
    (async () => {
      setD({
        atributos: await api('/api/sistemas/asus/atributos'),
        classes: await api('/api/sistemas/asus/classes?base=true'),
        pericias: await api('/api/sistemas/asus/pericias'),
        itens: await api('/api/sistemas/asus/itens'),
        progressao: await api('/api/sistemas/asus/progressao'),
        feiticos: await api('/api/sistemas/asus/feiticos/regras'),
      })
    })().catch((e) => setErro(e.message))
  }, [])

  return (
    <>
      <div className="page-head">
        <h1>Livro de Regras</h1>
        <span className="count-badge">ASUS</span>
      </div>
      {erro && <p className="error">{erro}</p>}
      <div className="abas">
        {ABAS.map((a) => (
          <button key={a} className={aba === a ? 'ativo' : undefined} onClick={() => setAba(a)}>{a}</button>
        ))}
      </div>

      {aba === 'Atributos' && (
        <div className="grid">
          {(d.atributos || []).map((a) => (
            <div key={a.codigo} className="card">
              <b>{a.nome} <span className="tag">{a.sigla}</span></b>
              <div className="muted">{a.descricao}</div>
            </div>
          ))}
        </div>
      )}

      {aba === 'Classes' && (
        <div className="grid">
          {(d.classes || []).map((c) => (
            <div key={c.codigo} className="card">
              <b>{c.nome}</b>
              <div className="muted">PV {c.multiplicadorPv} · PM {c.multiplicadorPm} · PE {c.multiplicadorPe}</div>
              <div className="muted" style={{ marginTop: 6 }}>{c.jsonPassiva}</div>
            </div>
          ))}
        </div>
      )}

      {aba === 'Perícias' && (
        <div className="card">
          <table><tbody>
            {(d.pericias || []).map((p) => (
              <tr key={p.codigo}><td>{p.nome}</td><td className="muted">{p.atributoBase}</td></tr>
            ))}
          </tbody></table>
        </div>
      )}

      {aba === 'Itens' && (
        <div className="card">
          <table>
            <thead><tr><th>Item</th><th>Categoria</th><th>Preço</th><th>Dano</th><th>Defesa</th></tr></thead>
            <tbody>
              {(d.itens || []).map((i) => (
                <tr key={i.codigo}>
                  <td>{i.nome}</td><td className="muted">{i.categoria}</td>
                  <td className="stat">{i.moeda} {i.preco}</td>
                  <td>{i.dano || '—'}{i.critico ? ` (${i.critico})` : ''}</td>
                  <td>{i.bonusDefesa != null ? `+${i.bonusDefesa}` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {aba === 'Progressão' && (
        <div className="card">
          <table>
            <thead><tr><th>Nível</th><th>XP</th><th>Foco</th><th>Recompensa</th><th>Lim. Atributo</th></tr></thead>
            <tbody>
              {(d.progressao || []).map((n) => (
                <tr key={n.nivel}>
                  <td>{n.nivel}</td><td className="stat">{n.xpNecessario}</td>
                  <td className="muted">{n.foco}</td><td className="muted">{n.recompensa}</td>
                  <td className="stat">{n.limiteAtributo}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {aba === 'Feitiços' && d.feiticos && (
        <div className="grid">
          {[
            ['Círculos', d.feiticos.circulos],
            ['Alcance / Área', d.feiticos.alcance],
            ['Poder — Dano', d.feiticos.poderDano],
            ['Poder — Cura', d.feiticos.poderCura],
            ['Duração', d.feiticos.duracao],
            ['Modificadores', d.feiticos.modificadores],
          ].map(([titulo, linhas]) => (
            <div key={titulo} className="card">
              <h2>{titulo}</h2>
              <table><tbody>
                {(linhas || []).map((l, i) => (
                  <tr key={i}><td>{l.nome}</td><td className="muted">{l.efeito}</td><td className="stat">{l.custoPm}</td></tr>
                ))}
              </tbody></table>
            </div>
          ))}
        </div>
      )}
    </>
  )
}
