import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'

export default function Escudo() {
  const { id } = useParams()
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [erro, setErro] = useState(null)

  const carregar = () =>
    api(`/api/campanhas/${id}/escudo?usuarioId=${user?.id}`).then(setData).catch((e) => setErro(e.message))
  useEffect(() => {
    if (user) carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user])

  async function ajustar(pid, campo, delta, atual) {
    try {
      await api(`/api/campanhas/${id}/escudo/personagens/${pid}/status?usuarioId=${user?.id}`, {
        method: 'PATCH',
        body: { [campo]: Math.max(0, atual + delta) },
      })
      carregar()
    } catch (e) {
      setErro(e.message)
    }
  }

  async function revelar(rid) {
    try {
      await api(`/api/campanhas/${id}/rolagens/${rid}/revelar?usuarioId=${user?.id}`, { method: 'POST' })
      carregar()
    } catch (e) {
      setErro(e.message)
    }
  }

  if (erro)
    return (
      <div>
        <p className="error">{erro}</p>
      </div>
    )
  if (!data) return <div className="center">Carregando…</div>
  return (
    <>
      <h1>Escudo — {data.campanha.nome}</h1>

      <div className="card">
        <h2>Personagens</h2>
        <table>
          <tbody>
            {data.personagens.map((p) => (
              <tr key={p.id}>
                <td>{p.nome}</td>
                <td className="stat">
                  PV {p.status.pvAtual}/{p.status.pvMax}
                </td>
                <td>
                  <button className="ghost" onClick={() => ajustar(p.id, 'pvAtual', -1, p.status.pvAtual)}>
                    -1 PV
                  </button>{' '}
                  <button className="ghost" onClick={() => ajustar(p.id, 'pvAtual', +1, p.status.pvAtual)}>
                    +1 PV
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2>Rolagens (todas)</h2>
        <table>
          <tbody>
            {data.rolagens.map((r) => (
              <tr key={r.id}>
                <td>{r.rotulo || r.expressao}</td>
                <td className="muted">{r.detalhe}</td>
                <td className="stat">{r.total}</td>
                <td>
                  {r.oculta && !r.revelada ? (
                    <button className="ghost" onClick={() => revelar(r.id)}>
                      Revelar
                    </button>
                  ) : r.oculta ? (
                    <span className="tag">revelada</span>
                  ) : (
                    ''
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
