import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'
import { useAuth } from '../auth.jsx'

export default function Campanha() {
  const { id } = useParams()
  const { user } = useAuth()
  const [campanha, setCampanha] = useState(null)
  const [rolagens, setRolagens] = useState([])
  const [membros, setMembros] = useState([])
  const [personagens, setPersonagens] = useState([])
  const [expr, setExpr] = useState('1d20')
  const [rotulo, setRotulo] = useState('')
  const [oculta, setOculta] = useState(false)
  const [convite, setConvite] = useState(null)
  const [erro, setErro] = useState(null)

  function carregar() {
    api(`/api/campanhas/${id}`).then(setCampanha).catch((e) => setErro(e.message))
    api(`/api/campanhas/${id}/rolagens`).then(setRolagens)
    api(`/api/campanhas/${id}/membros`).then(setMembros)
    api(`/api/campanhas/${id}/personagens`).then(setPersonagens)
  }
  useEffect(() => {
    carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  // Tempo real (Fase 6): novas rolagens entram no topo
  useEffect(
    () => inscrever(`/topic/campanhas/${id}/rolagens`, (r) => setRolagens((prev) => [r, ...prev])),
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
      <div className="row">
        <h1>{campanha.nome}</h1>
        <div className="spacer" />
        <Link className="tag" to={`/campanhas/${id}/escudo`}>
          Escudo do Mestre
        </Link>
        <Link className="tag" to={`/overlay/${id}`}>
          Overlay OBS
        </Link>
      </div>
      {erro && <p className="error">{erro}</p>}

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
            <input
              type="checkbox"
              style={{ width: 'auto' }}
              checked={oculta}
              onChange={(e) => setOculta(e.target.checked)}
            />{' '}
            oculta
          </label>
          <button style={{ alignSelf: 'end' }}>Rolar</button>
        </form>
      </div>

      <div className="card">
        <h2>Rolagens</h2>
        <table>
          <tbody>
            {rolagens.map((r) => (
              <tr key={r.id}>
                <td>{r.rotulo || r.expressao}</td>
                <td className="muted">{r.detalhe || (r.oculta ? '🎲 oculta' : '')}</td>
                <td
                  className="stat"
                  style={{
                    color: r.critico ? 'var(--crit)' : r.falhaCritica ? 'var(--fumble)' : 'inherit',
                  }}
                >
                  {r.total ?? '—'}
                  {r.critico ? ' ✦' : ''}
                  {r.falhaCritica ? ' ✗' : ''}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2>Membros</h2>
        <div className="row">
          {membros.map((m) => (
            <span key={m.id} className="tag">
              #{m.usuarioId} {m.papel}
            </span>
          ))}
        </div>
        <div style={{ marginTop: 10 }}>
          <button onClick={criarConvite}>Gerar convite</button>
          {convite && (
            <p className="muted">
              Código: <b>{convite.codigo}</b> (papel {convite.papel})
            </p>
          )}
        </div>
      </div>

      <div className="card">
        <h2>Personagens na campanha</h2>
        <div className="row">
          {personagens.map((cp) => (
            <Link key={cp.id} className="tag" to={`/personagens/${cp.personagemId}`}>
              {cp.personagemNome}
            </Link>
          ))}
        </div>
      </div>
    </>
  )
}
