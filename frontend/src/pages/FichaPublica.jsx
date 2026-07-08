import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import Heptagono from '../components/Heptagono.jsx'

const ATRIBS = [
  ['forca', 'For'], ['constituicao', 'Con'], ['destreza', 'Des'], ['agilidade', 'Agi'],
  ['inteligencia', 'Int'], ['sabedoria', 'Sab'], ['carisma', 'Car'],
]
const fmtEsp = (n) => String(Math.round((Number(n) || 0) * 100) / 100).replace('.', ',')

// Ficha compartilhada por link público: visão read-only, sem login.
export default function FichaPublica() {
  const { token } = useParams()
  const [d, setD] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    api(`/api/publico/personagens/${token}`, { auth: false }).then(setD).catch((e) => setErro(e.message))
  }, [token])

  if (erro) return <div className="container"><p className="error" style={{ marginTop: 40 }}>{erro}</p></div>
  if (!d) return <div className="center">Carregando…</div>

  const p = d.personagem
  const s = p.status || {}
  const barras = [
    ['Vida', 'vida', s.pvAtual, s.pvMax], ['Mana', 'mana', s.pmAtual, s.pmMax], ['Energia', 'energia', s.peAtual, s.peMax],
  ]

  return (
    <div className="container" style={{ maxWidth: 1100, margin: '0 auto', padding: '26px 18px 60px' }}>
      <div className="page-head">
        <h1>{p.nome}</h1>
        <span className="count-badge">
          {p.racaNome} · {p.classeNome}{p.trilhaNome ? ` · ${p.trilhaNome}` : ''} · Nv {p.nivel}
        </span>
        <div className="spacer" />
        <span className="tag">🔗 ficha pública (somente leitura)</span>
      </div>

      <div className="ficha">
        {/* Esquerda: identidade + atributos + status */}
        <div className="ficha-col">
          <div className="row" style={{ gap: 10 }}>
            <div className="avatar" style={{ overflow: 'hidden' }}>
              {p.avatarAssetId
                ? <img src={`/api/assets/${p.avatarAssetId}/conteudo`} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                : <b style={{ fontSize: 22 }}>{(p.nome || '?').charAt(0).toUpperCase()}</b>}
            </div>
            <div style={{ flex: 1 }}>
              <div className="kv"><b>Raça</b><span>{p.racaNome}</span></div>
              <div className="kv"><b>Classe</b><span>{p.classeNome}</span></div>
              {p.trilhaNome && <div className="kv"><b>Trilha</b><span>{p.trilhaNome}</span></div>}
              {p.divindade && <div className="kv"><b>Divindade</b><span>{p.divindade}</span></div>}
            </div>
          </div>

          <div className="mandala"><Heptagono atributos={p.atributosFinais} max={20} /></div>

          <div className="atr-edit">
            <b>Atributos</b>
            <div className="atr-grid">
              {ATRIBS.map(([k, sig]) => (
                <div key={k} className="atr-cell">
                  <span className="muted">{sig}</span>
                  <b className="stat" style={{ fontSize: '1.1rem' }}>{p.atributosFinais?.[k] ?? 0}</b>
                </div>
              ))}
            </div>
          </div>

          {barras.map(([rot, cls, atual, max]) => (
            <div key={rot} className="recurso">
              <div className="rec-rot">{rot}</div>
              <div className={`rec-bar ${cls}`}>
                <span className="rec-fill" style={{ width: (max > 0 ? Math.min(100, Math.round((atual / max) * 100)) : 0) + '%' }} />
                <b className="rec-val">{atual}/{max}</b>
              </div>
            </div>
          ))}
        </div>

        {/* Centro: perícias */}
        <div className="ficha-col">
          <h2>Perícias</h2>
          <table className="pericias">
            <thead><tr><th>Perícia</th><th>Atr</th><th>Treino</th><th>Outros</th></tr></thead>
            <tbody>
              {(p.pericias || []).map((pe) => (
                <tr key={pe.codigo} className={(pe.treino + (pe.bonus || 0) + (pe.outros || 0)) > 0 ? 'treinada' : undefined}>
                  <td>{pe.nome}{(pe.bonus || 0) > 0 && <span className="tag" style={{ marginLeft: 6 }}>+{pe.bonus}</span>}</td>
                  <td className="muted">{pe.sigla}</td>
                  <td className="stat">{pe.treino}</td>
                  <td className="stat">{pe.outros || 0}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Direita: listas */}
        <div className="ficha-col">
          {[['⚔ Ataques', d.ataques, (a) => `${a.nome} — ${a.dano || ''} ${a.critico ? `(${a.critico})` : ''}`],
            ['✦ Habilidades', d.habilidades, (h) => `${h.nome}${h.custo > 0 ? ` — ${h.custo} ${h.custoTipo}` : ''}`],
            ['🔮 Feitiços', d.feiticos, (f) => `${f.nome}${f.circulo ? ` — ${f.circulo}º` : ''}${f.custoPm ? ` · ${f.custoPm} PM` : ''}`],
            ['✨ Bênçãos', d.bencaos, (b) => `${b.nome}${b.divindade ? ` — ${b.divindade}` : ''}`],
            ['🎒 Inventário', d.inventario, (i) => `${i.nome} ×${i.quantidade || 1} (${fmtEsp(i.espacos)} esp)`],
          ].map(([titulo, lista, fmt]) => (
            <div key={titulo} style={{ marginBottom: 14 }}>
              <h2 style={{ marginBottom: 6 }}>{titulo}</h2>
              {(lista || []).length
                ? (lista || []).map((item, i) => (
                  <div key={i} className="item-card"><div className="t">{fmt(item)}</div>
                    {item.efeito && <div className="s muted">{item.efeito}</div>}
                  </div>
                ))
                : <div className="muted" style={{ fontSize: '.82rem' }}>—</div>}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
