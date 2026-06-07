import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api.js'
import Heptagono from '../components/Heptagono.jsx'

const ABAS = ['Combate', 'Habilidades', 'Magias', 'Inventário', 'Descrição']
const BARRAS = [['Vida', 'vida', 'pv'], ['Mana', 'mana', 'pm'], ['Energia', 'energia', 'pe']]
const ATRIBS = [
  ['forca', 'For'], ['constituicao', 'Con'], ['destreza', 'Des'], ['agilidade', 'Agi'],
  ['inteligencia', 'Int'], ['sabedoria', 'Sab'], ['carisma', 'Car'],
]
const PONTOS_ATRIBUTO = 5 // regra ASUS: 5 pontos distribuíveis sobre os fixos da classe
const CAMPOS_DESC = [
  ['anotacoes', 'Anotações'], ['aparencia', 'Aparência'], ['personalidade', 'Personalidade'],
  ['historico', 'Histórico'], ['objetivo', 'Objetivo'],
]
const vazioBase = { forca: 0, constituicao: 0, destreza: 0, agilidade: 0, inteligencia: 0, sabedoria: 0, carisma: 0 }

export default function Ficha() {
  const { id } = useParams()
  const [p, setP] = useState(null)
  const [aba, setAba] = useState('Combate')
  const [erro, setErro] = useState(null)
  const [treino, setTreino] = useState({})
  const [desc, setDesc] = useState({})
  const [base, setBase] = useState(vazioBase)
  const [nivelInput, setNivelInput] = useState('0')
  const [xpInput, setXpInput] = useState('0')
  const [statusInput, setStatusInput] = useState({ pvAtual: 0, pmAtual: 0, peAtual: 0 })
  const [levelUp, setLevelUp] = useState(null)
  const [habilidades, setHabilidades] = useState([])
  const [itens, setItens] = useState([])
  const [ataques, setAtaques] = useState([])
  const [feiticos, setFeiticos] = useState([])
  const [novoAtaque, setNovoAtaque] = useState({ nome: '', dano: '', critico: '', alcance: '' })
  const [novoFeitico, setNovoFeitico] = useState({ nome: '', circulo: 1, custoPm: 0, alcance: '', efeito: '' })

  function aplicar(d) {
    setP(d)
    setTreino(Object.fromEntries((d.pericias || []).map((pe) => [pe.codigo, pe.treino])))
    setDesc({
      anotacoes: d.anotacoes || '', aparencia: d.aparencia || '', personalidade: d.personalidade || '',
      historico: d.historico || '', objetivo: d.objetivo || '',
    })
    setBase({ ...vazioBase, ...(d.atributosBase || {}) })
    setNivelInput(String(d.nivel ?? 0))
    setXpInput(String(d.xpAtual ?? 0))
    setStatusInput({ pvAtual: d.status.pvAtual, pmAtual: d.status.pmAtual, peAtual: d.status.peAtual })
  }

  async function carregar() {
    const d = await api(`/api/personagens/${id}`)
    aplicar(d)
    return d
  }

  useEffect(() => {
    carregar()
      .then((d) => {
        if (d.classeCodigo) {
          api(`/api/sistemas/asus/habilidades?classe=${d.classeCodigo}`).then(setHabilidades).catch(() => {})
        }
        api('/api/sistemas/asus/itens').then(setItens).catch(() => {})
        api(`/api/personagens/${id}/ataques`).then(setAtaques).catch(() => {})
        api(`/api/personagens/${id}/feiticos`).then(setFeiticos).catch(() => {})
      })
      .catch((e) => setErro(e.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  // ----- status (PV/PM/PE) -----
  async function ajustar(campo, delta) {
    try {
      const novo = Math.max(0, p.status[campo] + delta)
      await api(`/api/personagens/${id}/status`, { method: 'PATCH', body: { [campo]: novo } })
      carregar()
    } catch (e) { setErro(e.message) }
  }
  async function definirStatus(campo) {
    try {
      const novo = Math.max(0, Number(statusInput[campo]) || 0)
      await api(`/api/personagens/${id}/status`, { method: 'PATCH', body: { [campo]: novo } })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  // ----- atributos (5 pontos distribuíveis) -----
  const totalBase = Object.values(base).reduce((a, b) => a + (Number(b) || 0), 0)
  function setAtr(attr, delta) {
    setBase((b) => {
      const atual = Number(b[attr]) || 0
      const novo = Math.max(0, atual + delta)
      const soma = totalBase - atual + novo
      if (delta > 0 && soma > PONTOS_ATRIBUTO) return b
      return { ...b, [attr]: novo }
    })
  }
  async function salvarAtributos() {
    try {
      await api(`/api/personagens/${id}`, { method: 'PUT', body: { atributosBase: base } })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  // ----- nível / XP (level-up automático + popup) -----
  async function salvarProgresso() {
    try {
      const resp = await api(`/api/personagens/${id}/progresso`, {
        method: 'PATCH',
        body: { xpAtual: Number(xpInput) || 0, nivel: Number(nivelInput) || 0 },
      })
      aplicar(resp.personagem)
      if (resp.niveisGanhos && resp.niveisGanhos.length) setLevelUp(resp.niveisGanhos)
    } catch (e) { setErro(e.message) }
  }

  // ----- foto -----
  async function trocarFoto(file) {
    if (!file) return
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('tipo', 'AVATAR_PERSONAGEM')
      const asset = await api(`/api/organizacoes/${p.organizacaoId}/assets`, { method: 'POST', body: form })
      await api(`/api/personagens/${id}`, { method: 'PUT', body: { avatarAssetId: asset.id } })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  const setTr = (cod, delta, cap) =>
    setTreino((t) => ({ ...t, [cod]: Math.max(0, Math.min(cap, (t[cod] || 0) + delta)) }))

  async function salvar(body) {
    try {
      await api(`/api/personagens/${id}`, { method: 'PUT', body })
      carregar()
    } catch (e) { setErro(e.message) }
  }

  const recarregarAtaques = () => api(`/api/personagens/${id}/ataques`).then(setAtaques).catch(() => {})
  const recarregarFeiticos = () => api(`/api/personagens/${id}/feiticos`).then(setFeiticos).catch(() => {})

  async function addAtaque() {
    if (!novoAtaque.nome.trim()) return
    try {
      await api(`/api/personagens/${id}/ataques`, { method: 'POST', body: novoAtaque })
      setNovoAtaque({ nome: '', dano: '', critico: '', alcance: '' })
      recarregarAtaques()
    } catch (e) { setErro(e.message) }
  }
  async function delAtaque(aid) {
    try { await api(`/api/ataques/${aid}`, { method: 'DELETE' }); recarregarAtaques() } catch (e) { setErro(e.message) }
  }
  async function addFeitico() {
    if (!novoFeitico.nome.trim()) return
    try {
      await api(`/api/personagens/${id}/feiticos`, {
        method: 'POST',
        body: { ...novoFeitico, circulo: Number(novoFeitico.circulo) || null, custoPm: Number(novoFeitico.custoPm) || null },
      })
      setNovoFeitico({ nome: '', circulo: 1, custoPm: 0, alcance: '', efeito: '' })
      recarregarFeiticos()
    } catch (e) { setErro(e.message) }
  }
  async function delFeitico(fid) {
    try { await api(`/api/feiticos/${fid}`, { method: 'DELETE' }); recarregarFeiticos() } catch (e) { setErro(e.message) }
  }

  if (erro) return <div><p className="error">{erro}</p></div>
  if (!p) return <div className="center">Carregando…</div>

  return (
    <>
      {levelUp && (
        <div className="modal" onClick={() => setLevelUp(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>⬆️ Subiu de nível!</h2>
            {levelUp.map((g) => (
              <div key={g.nivel} className="item-card">
                <div className="t">Nível {g.nivel}</div>
                <div className="s">{g.recompensa}{g.limiteAtributo ? ` · Teto de atributo: ${g.limiteAtributo}` : ''}</div>
              </div>
            ))}
            <button style={{ marginTop: 10 }} onClick={() => setLevelUp(null)}>Fechar</button>
          </div>
        </div>
      )}

      <h1>{p.nome}</h1>
      <div className="ficha">
        {/* Coluna esquerda: identidade, atributos e status */}
        <div className="ficha-col">
          <div className="row" style={{ gap: 10 }}>
            <label className="avatar" title="Trocar foto" style={{ cursor: 'pointer', overflow: 'hidden' }}>
              {p.avatarAssetId
                ? <img src={`/api/assets/${p.avatarAssetId}/conteudo`} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                : <span className="muted" style={{ fontSize: 11 }}>+ foto</span>}
              <input type="file" accept="image/*" style={{ display: 'none' }}
                onChange={(e) => trocarFoto(e.target.files[0])} />
            </label>
            <div style={{ flex: 1 }}>
              <div className="kv"><b>Raça</b><span>{p.racaNome}</span></div>
              <div className="kv"><b>Classe</b><span>{p.classeNome}</span></div>
              {p.trilhaNome && <div className="kv"><b>Trilha</b><span>{p.trilhaNome}</span></div>}
            </div>
          </div>

          <Heptagono atributos={p.atributosFinais} max={20} />

          {/* Editor de atributos (5 pontos distribuíveis sobre os fixos da classe) */}
          <div className="atr-edit">
            <div className="row">
              <b>Atributos</b>
              <div className="spacer" />
              <span className={`tag ${totalBase > PONTOS_ATRIBUTO ? 'over' : ''}`}>{totalBase}/{PONTOS_ATRIBUTO} pts</span>
              <button className="mini" onClick={salvarAtributos}>Salvar</button>
            </div>
            <div className="atr-grid">
              {ATRIBS.map(([k, sig]) => (
                <div key={k} className="atr-cell">
                  <span className="muted">{sig}</span>
                  <span className="step">
                    <button className="ghost mini" onClick={() => setAtr(k, -1)}>−</button>
                    <b className="stat">{base[k] ?? 0}</b>
                    <button className="ghost mini" onClick={() => setAtr(k, +1)}>+</button>
                  </span>
                  <span className="muted" title="final (base + fixos da classe)">= {p.atributosFinais[k]}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Nível e XP (level-up automático) */}
          <div className="atr-edit">
            <div className="row" style={{ gap: 8 }}>
              <label style={{ flex: 1 }}>Nível
                <input type="number" min="0" value={nivelInput} onChange={(e) => setNivelInput(e.target.value)} />
              </label>
              <label style={{ flex: 2 }}>XP
                <input type="number" min="0" value={xpInput} onChange={(e) => setXpInput(e.target.value)} />
              </label>
              <button className="mini" style={{ alignSelf: 'flex-end' }} onClick={salvarProgresso}>Salvar</button>
            </div>
          </div>

          <div className="row">
            <span className="tag">Deslocamento {p.deslocamento}m</span>
            <span className="tag">Carga máx {p.cargaMaxima}</span>
          </div>

          {BARRAS.map(([rot, cls, k]) => {
            const atual = p.status[k + 'Atual']
            const max = p.status[k + 'Max']
            const pct = max > 0 ? Math.round((atual / max) * 100) : 0
            return (
              <div key={k} style={{ marginTop: 8 }}>
                <div className="bar-label">{rot}</div>
                <div className="row" style={{ gap: 6, flexWrap: 'nowrap' }}>
                  <button className="ghost mini" onClick={() => ajustar(k + 'Atual', -1)}>−</button>
                  <div className={`bar ${cls}`} style={{ flex: 1 }}>
                    <span style={{ width: pct + '%' }} /><b>{atual}/{max}</b>
                  </div>
                  <button className="ghost mini" onClick={() => ajustar(k + 'Atual', +1)}>+</button>
                  <input className="mini-num" type="number" min="0" value={statusInput[k + 'Atual']}
                    onChange={(e) => setStatusInput((s) => ({ ...s, [k + 'Atual']: e.target.value }))}
                    onBlur={() => definirStatus(k + 'Atual')} title="definir valor atual" />
                </div>
              </div>
            )
          })}

          <div className="kv" style={{ marginTop: 10 }}><b>Limites</b><span>Hab {p.limiteHabilidades} · Fei {p.limiteFeiticos} · Bên {p.limiteBencaos}</span></div>
        </div>

        {/* Centro: perícias com treino */}
        <div className="ficha-col">
          <div className="row">
            <h2>Perícias</h2>
            <div className="spacer" />
            <button className="mini" onClick={() => salvar({ pericias: treino })}>Salvar</button>
          </div>
          <table className="pericias">
            <thead><tr><th>Perícia</th><th>Atr</th><th>Treino</th><th>Teto</th></tr></thead>
            <tbody>
              {p.pericias.map((pe) => (
                <tr key={pe.codigo}>
                  <td>{pe.nome}</td>
                  <td className="muted">{pe.sigla}</td>
                  <td>
                    <span className="step">
                      <button className="ghost mini" onClick={() => setTr(pe.codigo, -1, pe.cap)}>−</button>
                      <b className="stat">{treino[pe.codigo] ?? 0}</b>
                      <button className="ghost mini" onClick={() => setTr(pe.codigo, +1, pe.cap)}>+</button>
                    </span>
                  </td>
                  <td className="muted stat">{pe.cap}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Direita: abas */}
        <div className="ficha-col">
          <div className="abas">
            {ABAS.map((x) => (
              <button key={x} className={aba === x ? 'ativo' : undefined} onClick={() => setAba(x)}>{x}</button>
            ))}
          </div>

          {aba === 'Combate' && (
            <div>
              {ataques.map((a) => (
                <div key={a.id} className="item-card">
                  <div className="t">
                    {a.nome} {a.dano && <span className="tag">{a.dano}</span>}
                    <button className="ghost mini" style={{ float: 'right' }} onClick={() => delAtaque(a.id)}>✕</button>
                  </div>
                  <div className="s">
                    {[a.critico && `Crít ${a.critico}`, a.alcance, a.pericia].filter(Boolean).join(' · ')}
                    {a.efeito ? ` · ${a.efeito}` : ''}
                  </div>
                </div>
              ))}
              {!ataques.length && <div className="muted">Nenhum ataque cadastrado.</div>}
              <div className="add-form">
                <input placeholder="Nome" value={novoAtaque.nome}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, nome: e.target.value }))} />
                <input placeholder="Dano (1d8)" value={novoAtaque.dano}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, dano: e.target.value }))} />
                <input placeholder="Crítico (x2)" value={novoAtaque.critico}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, critico: e.target.value }))} />
                <input placeholder="Alcance" value={novoAtaque.alcance}
                  onChange={(e) => setNovoAtaque((s) => ({ ...s, alcance: e.target.value }))} />
                <button className="mini" onClick={addAtaque}>+ Ataque</button>
              </div>
              <div className="muted" style={{ marginTop: 8 }}>Para rolar dados em mesa, abra a campanha.</div>
            </div>
          )}

          {aba === 'Habilidades' && (
            habilidades.length ? habilidades.map((h) => (
              <div key={h.id} className="item-card">
                <div className="t">{h.nome} <span className="tag">{h.tipo}</span></div>
                <div className="s">{h.custo > 0 ? `${h.custo} ${h.custoTipo} · ` : ''}{h.efeito}</div>
              </div>
            )) : <div className="muted">Sem habilidades de classe listadas para {p.classeNome}.</div>
          )}

          {aba === 'Magias' && (
            <div>
              <div className="muted" style={{ marginBottom: 8 }}>
                Construa feitiços pelas regras em <b>Livros → Feitiços</b> (círculo, alcance, poder, duração).
              </div>
              {feiticos.map((f) => (
                <div key={f.id} className="item-card">
                  <div className="t">
                    {f.nome} {f.circulo ? <span className="tag">{f.circulo}º círculo</span> : null}
                    <button className="ghost mini" style={{ float: 'right' }} onClick={() => delFeitico(f.id)}>✕</button>
                  </div>
                  <div className="s">
                    {[f.custoPm ? `${f.custoPm} PM` : null, f.alcance, f.duracao].filter(Boolean).join(' · ')}
                    {f.efeito ? ` · ${f.efeito}` : ''}
                  </div>
                </div>
              ))}
              {!feiticos.length && <div className="muted">Nenhum feitiço cadastrado.</div>}
              <div className="add-form">
                <input placeholder="Nome" value={novoFeitico.nome}
                  onChange={(e) => setNovoFeitico((s) => ({ ...s, nome: e.target.value }))} />
                <input type="number" min="1" max="5" placeholder="Círculo" style={{ maxWidth: 90 }} value={novoFeitico.circulo}
                  onChange={(e) => setNovoFeitico((s) => ({ ...s, circulo: e.target.value }))} />
                <input type="number" min="0" placeholder="PM" style={{ maxWidth: 80 }} value={novoFeitico.custoPm}
                  onChange={(e) => setNovoFeitico((s) => ({ ...s, custoPm: e.target.value }))} />
                <input placeholder="Alcance" value={novoFeitico.alcance}
                  onChange={(e) => setNovoFeitico((s) => ({ ...s, alcance: e.target.value }))} />
                <input placeholder="Efeito" value={novoFeitico.efeito}
                  onChange={(e) => setNovoFeitico((s) => ({ ...s, efeito: e.target.value }))} />
                <button className="mini" onClick={addFeitico}>+ Feitiço</button>
              </div>
            </div>
          )}

          {aba === 'Inventário' && (
            <div>
              <div className="muted" style={{ marginBottom: 8 }}>
                Catálogo do sistema (itens de Tormenta 20). Inventário editável e itens próprios chegam na próxima etapa.
              </div>
              {itens.slice(0, 30).map((i) => (
                <div key={i.id} className="item-card">
                  <div className="t">{i.nome}</div>
                  <div className="s">{i.categoria} · {i.moeda} {i.preco}{i.dano ? ` · ${i.dano}` : ''}</div>
                </div>
              ))}
            </div>
          )}

          {aba === 'Descrição' && (
            <div>
              {CAMPOS_DESC.map(([k, rot]) => (
                <div key={k}>
                  <label>{rot}</label>
                  <textarea value={desc[k]} onChange={(e) => setDesc((dd) => ({ ...dd, [k]: e.target.value }))} />
                </div>
              ))}
              <button style={{ marginTop: 10 }} onClick={() => salvar(desc)}>Salvar descrição</button>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
