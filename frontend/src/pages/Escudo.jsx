import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { inscrever } from '../ws.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'
import ResultadosPanel from '../components/ResultadosPanel.jsx'

const ATRIBS = [
  ['forca', 'FOR'], ['constituicao', 'CON'], ['destreza', 'DES'], ['agilidade', 'AGI'],
  ['inteligencia', 'INT'], ['sabedoria', 'SAB'], ['carisma', 'CAR'],
]
const BARRAS = [['Vida', 'vida', 'pv'], ['Mana', 'mana', 'pm'], ['Energia', 'energia', 'pe']]

function Recurso({ label, cls, atual, max, onMinus, onPlus }) {
  const a = atual ?? 0
  const m = max ?? 0
  const pct = m > 0 ? Math.max(0, Math.min(100, (a / m) * 100)) : 0
  return (
    <div className="res">
      <div className="lbl"><span>{label}</span><span>{a}/{m}</span></div>
      <div className={`bar sm ${cls}`} style={{ position: 'relative' }}>
        <span style={{ width: `${pct}%` }} />
        <button className="rec-step" style={{ position: 'absolute', left: 0, top: 0, bottom: 0 }} onClick={onMinus}>‹</button>
        <button className="rec-step" style={{ position: 'absolute', right: 0, top: 0, bottom: 0 }} onClick={onPlus}>›</button>
      </div>
    </div>
  )
}

export default function Escudo() {
  const { id } = useParams()
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [rolagens, setRolagens] = useState([])
  const [erro, setErro] = useState(null)

  const carregarRolagens = () =>
    api(`/api/campanhas/${id}/escudo/rolagens?usuarioId=${user?.id}`).then(setRolagens).catch(() => {})

  const carregar = () =>
    api(`/api/campanhas/${id}/escudo?usuarioId=${user?.id}`)
      .then((d) => { setData(d); setRolagens(d.rolagens || []) })
      .catch((e) => setErro(e.message))

  useEffect(() => {
    if (user) carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user])

  // Tempo real: ao chegar uma rolagem nova, recarrega o histórico completo (mestre vê tudo).
  useEffect(() => {
    if (!user) return undefined
    return inscrever(`/topic/campanhas/${id}/rolagens`, () => carregarRolagens())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user])

  // Status em tempo real: quando um jogador edita a própria ficha, o card do escudo atualiza.
  const idsPersonagens = (data?.personagens || []).map((pp) => pp.id).join(',')
  useEffect(() => {
    const ids = idsPersonagens ? idsPersonagens.split(',') : []
    if (!ids.length) return undefined
    const subs = ids.map((pid) =>
      inscrever(`/topic/personagens/${pid}/status`, (s) =>
        setData((prev) => (prev ? {
          ...prev,
          personagens: prev.personagens.map((x) =>
            String(x.id) === String(pid) ? { ...x, status: { ...x.status, ...s } } : x),
        } : prev)),
      ),
    )
    return () => subs.forEach((u) => u && u())
  }, [idsPersonagens])

  async function ajustar(pid, campo, delta, atual) {
    try {
      await api(`/api/campanhas/${id}/escudo/personagens/${pid}/status?usuarioId=${user?.id}`, {
        method: 'PATCH',
        body: { [campo]: Math.max(0, (atual ?? 0) + delta) },
      })
      carregar()
    } catch (e) { setErro(e.message) }
  }
  async function revelar(rid) {
    try {
      await api(`/api/campanhas/${id}/rolagens/${rid}/revelar?usuarioId=${user?.id}`, { method: 'POST' })
      carregarRolagens()
    } catch (e) { setErro(e.message) }
  }
  async function rolar(expressao, rotulo, privada) {
    await api(`/api/campanhas/${id}/rolagens`, {
      method: 'POST',
      body: { expressao, rotulo, oculta: !!privada, usuarioId: user?.id },
    })
    carregarRolagens()
  }

  if (erro) return <div><p className="error">{erro}</p></div>
  if (!data) return <div className="center">Carregando…</div>

  return (
    <>
      <div className="page-head">
        <h1>{data.campanha.nome}</h1>
        <span className="count-badge">Escudo do Mestre</span>
      </div>

      <div className="abas" style={{ marginBottom: 16 }}>
        <button className="ativo">Agentes ({data.personagens.length})</button>
        <Link to={`/campanhas/${id}`} className="tag" style={{ marginLeft: 'auto', alignSelf: 'center' }}>
          ← Voltar à campanha
        </Link>
      </div>

      <div className="vtt-grid">
        {data.personagens.map((p) => {
          const s = p.status || {}
          const at = p.atributosFinais || {}
          return (
            <div key={p.id} className="vtt-card">
              <div className="head">
                <div className="av"
                  style={p.avatarAssetId ? { backgroundImage: `url(/api/assets/${p.avatarAssetId}/conteudo)` } : undefined}>
                  {!p.avatarAssetId && (p.nome || '?').charAt(0).toUpperCase()}
                </div>
                <div style={{ minWidth: 0 }}>
                  <div className="nm">{p.nome}</div>
                  <div className="muted" style={{ fontSize: '.76rem' }}>
                    {[p.classeNome, p.racaNome, p.nivel ? `Nv ${p.nivel}` : null].filter(Boolean).join(' · ')}
                  </div>
                  {p.jogador && <div className="muted" style={{ fontSize: '.72rem' }}>Jogador: {p.jogador}</div>}
                </div>
              </div>

              <div className="atribs">
                {ATRIBS.map(([k, sig]) => (
                  <div key={k} className="at"><span className="k">{sig}</span><span className="v">{at[k] ?? 0}</span></div>
                ))}
              </div>

              {BARRAS.map(([rot, cls, k]) => (
                s[k + 'Max'] != null && (
                  <Recurso key={k} label={rot} cls={cls} atual={s[k + 'Atual']} max={s[k + 'Max']}
                    onMinus={() => ajustar(p.id, k + 'Atual', -1, s[k + 'Atual'])}
                    onPlus={() => ajustar(p.id, k + 'Atual', +1, s[k + 'Atual'])} />
                )
              ))}

              <div className="deriv">
                <span>DESL <b>{p.deslocamento}m</b></span>
                <span>Carga <b>{p.cargaAtual}/{p.cargaMaxima}</b></span>
                <div className="spacer" />
                <Link to={`/personagens/${p.id}`} className="tag">Ficha</Link>
              </div>
            </div>
          )
        })}
        {data.personagens.length === 0 && <p className="muted">Nenhum personagem na campanha.</p>}
      </div>

      {/* Rolagens dos jogadores (o mestre vê tudo, inclusive as privadas) */}
      <div className="card" style={{ marginTop: 16 }}>
        <div className="row"><h2 style={{ margin: 0 }}>Rolagens dos jogadores</h2>
          <span className="muted" style={{ fontSize: '.78rem', alignSelf: 'center' }}>· em tempo real</span></div>
        <div className="lista-vert" style={{ maxHeight: 380, overflow: 'auto', marginTop: 10 }}>
          {rolagens.map((r) => {
            const oculto = r.oculta && r.total == null
            const cor = r.critico ? 'var(--crit, #4ad06a)' : r.falhaCritica ? 'var(--fumble, #e0554a)' : 'var(--text)'
            return (
              <div key={r.id} className="sessao-row">
                <div className="hex" style={{ fontWeight: 700, fontSize: '1.1rem', minWidth: 40, textAlign: 'center', color: oculto ? 'var(--muted)' : cor }}>
                  {oculto ? '🎲' : (r.total ?? '—')}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
                    {r.personagemNome && <b style={{ color: 'var(--gold, #e0b64a)' }}>{r.personagemNome}</b>}
                    <span>{r.rotulo || r.expressao}</span>
                    {r.critico && <span className="tag">crítico</span>}
                    {r.falhaCritica && <span className="tag">falha</span>}
                    {r.oculta && <span className="tag">privada</span>}
                  </div>
                  <div className="muted" style={{ fontSize: '.72rem' }}>
                    {oculto ? 'rolagem privada' : (r.detalhe || r.expressao)} · {dataHora(r.criadoEm)}
                  </div>
                </div>
                {r.oculta && !r.revelada && (
                  <button className="ghost mini" onClick={() => revelar(r.id)} title="Revelar para todos">👁 Revelar</button>
                )}
              </div>
            )
          })}
          {!rolagens.length && <div className="muted">Nenhuma rolagem ainda.</div>}
        </div>
      </div>

      <ResultadosPanel rolagens={rolagens} onRolar={rolar} ehMestre onRevelar={revelar} />
    </>
  )
}
