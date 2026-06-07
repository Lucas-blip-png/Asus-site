import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'

function Bar({ tipo, atual, max }) {
  const pct = max > 0 ? Math.round((atual / max) * 100) : 0
  return (
    <div className={`bar ${tipo}`}>
      <span style={{ width: pct + '%' }} />
    </div>
  )
}

export default function Personagem() {
  const { id } = useParams()
  const [p, setP] = useState(null)
  const [erro, setErro] = useState(null)

  const carregar = () => api(`/api/personagens/${id}`).then(setP).catch((e) => setErro(e.message))
  useEffect(() => {
    carregar()
  }, [id])

  async function ajustar(campo, delta) {
    const atual = p.status[campo]
    try {
      await api(`/api/personagens/${id}/status`, { method: 'PATCH', body: { [campo]: Math.max(0, atual + delta) } })
      carregar()
    } catch (e) {
      setErro(e.message)
    }
  }

  if (!p) return <div className="center">Carregando…</div>
  const a = p.atributosFinais
  return (
    <>
      <div className="row">
        <h1>{p.nome}</h1>
        <span className="tag">{p.racaNome} · {p.classeNome} · Nv {p.nivel}</span>
      </div>
      {erro && <p className="error">{erro}</p>}

      <div className="card">
        <h2>Status</h2>
        {['pv', 'pm', 'pe'].map((t) => (
          <div key={t} style={{ marginBottom: 10 }}>
            <div className="row">
              <b style={{ textTransform: 'uppercase' }}>{t}</b>
              <span className="stat">
                {p.status[t + 'Atual']}/{p.status[t + 'Max']}
              </span>
              <div className="spacer" />
              <button className="ghost" onClick={() => ajustar(t + 'Atual', -1)}>-1</button>
              <button className="ghost" onClick={() => ajustar(t + 'Atual', +1)}>+1</button>
            </div>
            <Bar tipo={t} atual={p.status[t + 'Atual']} max={p.status[t + 'Max']} />
          </div>
        ))}
        <div className="muted">Defesa {p.status.defesa}</div>
      </div>

      <div className="card">
        <h2>Atributos</h2>
        <div className="row">
          {['forca', 'agilidade', 'vigor', 'intelecto', 'presenca'].map((at) => (
            <span key={at} className="tag">
              {at.slice(0, 3).toUpperCase()} {a[at]}
            </span>
          ))}
        </div>
      </div>

      <div className="card">
        <h2>Perícias</h2>
        <table>
          <tbody>
            {p.pericias.map((pe) => (
              <tr key={pe.codigo}>
                <td>{pe.nome}</td>
                <td className="muted">{pe.atributoBase}</td>
                <td className="stat">
                  {pe.valor >= 0 ? '+' : ''}
                  {pe.valor}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
