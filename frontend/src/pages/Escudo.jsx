import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'

function Recurso({ label, cls, atual, max }) {
  const a = atual ?? 0
  const m = max ?? 0
  const pct = m > 0 ? Math.max(0, Math.min(100, (a / m) * 100)) : 0
  return (
    <div className="res">
      <div className="lbl"><span>{label}</span><span>{a}/{m}</span></div>
      <div className={`bar sm ${cls}`}><span style={{ width: `${pct}%` }} /></div>
    </div>
  )
}

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
        body: { [campo]: Math.max(0, (atual ?? 0) + delta) },
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
      <div className="page-head">
        <h1>Escudo do Mestre</h1>
        <span className="count-badge">{data.campanha.nome}</span>
      </div>

      <h2>Personagens</h2>
      <div className="vtt-grid">
        {data.personagens.map((p) => {
          const s = p.status || {}
          return (
            <div key={p.id} className="vtt-card">
              <div className="head">
                <div
                  className="av"
                  style={p.avatarAssetId ? { backgroundImage: `url(/api/assets/${p.avatarAssetId}/conteudo)` } : undefined}
                >
                  {!p.avatarAssetId && (p.nome || '?').charAt(0).toUpperCase()}
                </div>
                <div>
                  <div className="nm">{p.nome}</div>
                  {p.classeNome && (
                    <div className="muted" style={{ fontSize: '.78rem' }}>
                      {p.classeNome}{p.nivel ? ` · Nv ${p.nivel}` : ''}
                    </div>
                  )}
                </div>
              </div>
              <Recurso label="PV" cls="vida" atual={s.pvAtual} max={s.pvMax} />
              {s.pmMax != null && <Recurso label="PM" cls="mana" atual={s.pmAtual} max={s.pmMax} />}
              {s.peMax != null && <Recurso label="PE" cls="energia" atual={s.peAtual} max={s.peMax} />}
              <div className="ctrls">
                <button className="ghost mini" onClick={() => ajustar(p.id, 'pvAtual', -1, s.pvAtual)}>−PV</button>
                <button className="ghost mini" onClick={() => ajustar(p.id, 'pvAtual', +1, s.pvAtual)}>+PV</button>
                {s.pmMax != null && (
                  <>
                    <button className="ghost mini" onClick={() => ajustar(p.id, 'pmAtual', -1, s.pmAtual)}>−PM</button>
                    <button className="ghost mini" onClick={() => ajustar(p.id, 'pmAtual', +1, s.pmAtual)}>+PM</button>
                  </>
                )}
                {s.peMax != null && (
                  <>
                    <button className="ghost mini" onClick={() => ajustar(p.id, 'peAtual', -1, s.peAtual)}>−PE</button>
                    <button className="ghost mini" onClick={() => ajustar(p.id, 'peAtual', +1, s.peAtual)}>+PE</button>
                  </>
                )}
              </div>
            </div>
          )
        })}
        {data.personagens.length === 0 && <p className="muted">Nenhum personagem na campanha.</p>}
      </div>

      <div className="card" style={{ marginTop: 18 }}>
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
                    <button className="ghost mini" onClick={() => revelar(r.id)}>
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
