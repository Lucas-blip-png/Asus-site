import { useEffect, useState } from 'react'
import { api } from '../api.js'

const ABAS = ['Atributos', 'Classes', 'Habilidades', 'Perícias', 'Itens', 'Progressão', 'Feitiços']

const ATR_SIGLA = {
  FORCA: 'FOR', CONSTITUICAO: 'CON', DESTREZA: 'DES', AGILIDADE: 'AGI',
  INTELIGENCIA: 'INT', SABEDORIA: 'SAB', CARISMA: 'CAR',
}

const ITEM_TABS = [
  ['Armas Simples', ['ARMA_SIMPLES']],
  ['Armas Marciais', ['ARMA_MARCIAL']],
  ['Armaduras & Escudos', ['ARMADURA', 'ESCUDO']],
  ['Itens Gerais', ['ITEM_GERAL', 'FERRAMENTA']],
  ['Alquímicos', ['ALQUIMICO', 'VENENO']],
  ['Alimentação & Mais', ['ALIMENTACAO', 'ANIMAL', 'VEICULO', 'SERVICO']],
]

const precoFmt = (i) =>
  i.preco == null ? '—' : `${i.moeda || 'T$'}${Number(i.preco).toLocaleString('pt-BR', { maximumFractionDigits: 2 })}`
const espFmt = (n) => (n == null ? '—' : String(n).replace('.', ','))
const tipoClasse = (t) => {
  const s = (t || '').toLowerCase()
  if (s.includes('perfura') && s.includes('corte')) return 'td-mix'
  if (s.includes('perfura')) return 'td-perf'
  if (s.includes('corte')) return 'td-corte'
  if (s.includes('impacto')) return 'td-impacto'
  return ''
}
function layoutDe(items) {
  if (items.some((i) => i.dano)) return 'arma'
  if (items.some((i) => i.bonusDefesa != null)) return items.some((i) => i.penalidade != null) ? 'armadura' : 'escudo'
  if (items.some((i) => i.espacos != null)) return 'espacos'
  return 'preco'
}

function ItensView({ itens }) {
  const [tab, setTab] = useState(0)
  const cats = ITEM_TABS[tab][1]
  const doTab = itens.filter((i) => cats.includes(i.categoria))
  // Agrupa por 'grupo' preservando a ordem de chegada (ordem do seed).
  const grupos = []
  const idx = {}
  doTab.forEach((i) => {
    const g = i.grupo || 'Itens'
    if (idx[g] == null) { idx[g] = grupos.length; grupos.push([g, []]) }
    grupos[idx[g]][1].push(i)
  })

  return (
    <div>
      <div className="abas" style={{ marginBottom: 14 }}>
        {ITEM_TABS.map(([nome], i) => (
          <button key={nome} className={tab === i ? 'ativo' : undefined} onClick={() => setTab(i)}>{nome}</button>
        ))}
      </div>
      {grupos.map(([g, items]) => {
        const lay = layoutDe(items)
        return (
          <div key={g} className="card" style={{ marginBottom: 14 }}>
            <h2 style={{ marginTop: 0 }}>{g}</h2>
            <table>
              <thead>
                {lay === 'arma' && <tr><th>Item</th><th>Preço</th><th>Dano</th><th>Crítico</th><th>Alcance</th><th>Tipo</th></tr>}
                {lay === 'armadura' && <tr><th>Item</th><th>Preço</th><th>Defesa</th><th>Penalidade</th><th>Espaços</th></tr>}
                {lay === 'escudo' && <tr><th>Item</th><th>Preço</th><th>Defesa</th><th>Espaços</th></tr>}
                {lay === 'espacos' && <tr><th>Item</th><th>Preço</th><th>Espaços</th></tr>}
                {lay === 'preco' && <tr><th>Item</th><th>Preço</th></tr>}
              </thead>
              <tbody>
                {items.map((i) => (
                  <tr key={i.codigo}>
                    <td>{i.nome}</td>
                    <td className="stat">{precoFmt(i)}</td>
                    {lay === 'arma' && <>
                      <td>{i.dano || '—'}</td>
                      <td className="muted">{i.critico || '—'}</td>
                      <td className="muted">{i.alcance || '—'}</td>
                      <td><span className={`td-badge ${tipoClasse(i.tipoDano)}`}>{i.tipoDano || '—'}</span></td>
                    </>}
                    {lay === 'armadura' && <>
                      <td className="stat">+{i.bonusDefesa}</td>
                      <td className="muted">{i.penalidade ? i.penalidade : (i.penalidade === 0 ? '0' : '—')}</td>
                      <td className="muted">{espFmt(i.espacos)}</td>
                    </>}
                    {lay === 'escudo' && <>
                      <td className="stat">+{i.bonusDefesa}</td>
                      <td className="muted">{espFmt(i.espacos)}</td>
                    </>}
                    {lay === 'espacos' && <td className="muted">{espFmt(i.espacos)}</td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      })}
    </div>
  )
}

function PericiaCard({ p }) {
  const [open, setOpen] = useState(true)
  const sig = ATR_SIGLA[p.atributoBase] || p.atributoBase || '—'
  const exemplos = (p.exemplos || '').split('|').filter(Boolean)
  return (
    <div className="pericia-card">
      <div className="pc-head" onClick={() => setOpen((o) => !o)}>
        <span className={`atr-badge a-${sig.toLowerCase()}`}>{sig}</span>
        <b className="pc-nome">{p.nome}</b>
        <span className="muted pc-max">Máx: Dobro do {sig}</span>
        <span className="chev">{open ? '▴' : '▾'}</span>
      </div>
      {open && (
        <div className="pc-body">
          {p.descricao && <p className="pc-desc">{p.descricao}</p>}
          {exemplos.length > 0 && (
            <div className="pc-ex">
              {exemplos.map((e, i) => <span key={i} className="pc-chip">{e}</span>)}
            </div>
          )}
          <div className="pc-foot">LIMITE MÁXIMO: DOBRO DO {sig}</div>
        </div>
      )}
    </div>
  )
}

function ClasseCard({ c, trilhas }) {
  const [open, setOpen] = useState(false)
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{c.nome}</b>
        <span className="sub">PV {c.multiplicadorPv} · PM {c.multiplicadorPm} · PE {c.multiplicadorPe}</span>
        <div className="spacer" />
        {trilhas.length > 0 && <span className="tag">{trilhas.length} trilha{trilhas.length > 1 ? 's' : ''}</span>}
      </div>
      {open && (
        <div className="cris-body">
          {c.jsonPassiva && (
            <p style={{ margin: '2px 0 8px' }}><b>Passiva.</b> <span className="muted">{c.jsonPassiva}</span></p>
          )}
          {trilhas.length > 0 && (
            <div>
              <div className="muted" style={{ textTransform: 'uppercase', fontSize: '.7rem', letterSpacing: 1, marginBottom: 4 }}>Trilhas</div>
              {trilhas.map((t) => (
                <div key={t.codigo} className="item-card">
                  <div className="t">{t.nome}</div>
                  {t.jsonPassiva && <div className="s"><b>Passiva:</b> {t.jsonPassiva}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const fmtClasses = (cods) => (cods || '').split(',').map((c) => c.trim()).filter(Boolean)
  .map((c) => (c === 'GERAL' ? 'Todas as classes'
    : c.split('_').map((w) => (w ? w[0] + w.slice(1).toLowerCase() : '')).join(' '))).join(', ')
const capitaliza = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : s)

function HabilidadeRow({ h }) {
  const [open, setOpen] = useState(false)
  const geral = !h.classeCodigo || h.classeCodigo === 'GERAL'
  const tipoLabel = (h.tipo || '').toUpperCase() === 'PASSIVA' ? 'Passiva' : 'Ativa'
  return (
    <div className={`cris-row${open ? ' open' : ''}`}>
      <div className="cris-head" onClick={() => setOpen((o) => !o)}>
        <span className="chev">▾</span>
        <b className="nm">{h.nome}</b>
        <span className="sub">{geral ? 'Todas as classes' : fmtClasses(h.classeCodigo)}</span>
        <div className="spacer" />
        {h.tipo && <span className="tag">{tipoLabel}</span>}
        {h.custo > 0 && <span className="tag">{h.custo} {h.custoTipo}</span>}
      </div>
      {open && (
        <div className="cris-body">
          <div className="row" style={{ gap: 6, flexWrap: 'wrap', marginBottom: 6 }}>
            <span className="tag">{geral ? 'Todas as classes' : fmtClasses(h.classeCodigo)}</span>
            {h.tipo && <span className="tag">{tipoLabel}</span>}
            {h.custo > 0 && <span className="tag">Custo {h.custo} {h.custoTipo}</span>}
            {h.nivelMinimo > 1 && <span className="tag">Nível {h.nivelMinimo}</span>}
            {h.atributoRequisito && (
              <span className="tag">Requer {h.valorAtributoRequisito} de {capitaliza(h.atributoRequisito)}</span>
            )}
          </div>
          {h.requisito && <p className="muted" style={{ fontSize: '.78rem', margin: '0 0 6px' }}><b>Requisito:</b> {h.requisito}</p>}
          {h.efeito && <p style={{ margin: 0, fontSize: '.86rem', lineHeight: 1.5 }}>{h.efeito}</p>}
        </div>
      )}
    </div>
  )
}

function HabilidadesView({ habilidades, classes }) {
  const [busca, setBusca] = useState('')
  const [classe, setClasse] = useState('')
  const [tipo, setTipo] = useState('')
  const bases = (classes || []).filter((c) => !c.classePaiCodigo)
  const trilhas = (classes || []).filter((c) => c.classePaiCodigo)
  const q = busca.trim().toLowerCase()

  const filtradas = (habilidades || []).filter((h) => {
    if (q && !(`${h.nome || ''} ${h.efeito || ''} ${fmtClasses(h.classeCodigo)}`).toLowerCase().includes(q)) return false
    if (tipo && (h.tipo || '').toUpperCase() !== tipo) return false
    if (classe) {
      const donos = (h.classeCodigo || '').split(',').map((s) => s.trim()).filter(Boolean)
      const geral = donos.length === 0 || donos.includes('GERAL')
      if (classe === 'GERAL') return geral
      // classe específica: mostra as da classe + as gerais (que valem pra todas)
      return donos.includes(classe) || geral
    }
    return true
  })

  return (
    <div>
      <div className="row" style={{ gap: 8, marginBottom: 14, flexWrap: 'wrap' }}>
        <div className="search-wrap" style={{ flex: '1 1 220px' }}>
          <span className="ic">🔍</span>
          <input placeholder="Buscar habilidade…" value={busca} onChange={(e) => setBusca(e.target.value)} />
        </div>
        <select value={classe} onChange={(e) => setClasse(e.target.value)}>
          <option value="">Todas as classes</option>
          <option value="GERAL">Gerais (todas as classes)</option>
          <optgroup label="Classes">
            {bases.map((c) => <option key={c.codigo} value={c.codigo}>{c.nome}</option>)}
          </optgroup>
          <optgroup label="Trilhas">
            {trilhas.map((t) => <option key={t.codigo} value={t.codigo}>{t.nome}</option>)}
          </optgroup>
        </select>
        <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
          <option value="">Todos os tipos</option>
          <option value="ATIVA">Ativas</option>
          <option value="PASSIVA">Passivas</option>
        </select>
      </div>
      <div className="muted" style={{ marginBottom: 8 }}>
        {filtradas.length} habilidade{filtradas.length === 1 ? '' : 's'}
      </div>
      <div className="cris-list">
        {filtradas.map((h) => <HabilidadeRow key={h.codigo} h={h} />)}
        {!filtradas.length && <div className="muted">Nenhuma habilidade encontrada.</div>}
      </div>
    </div>
  )
}

export default function Livros() {
  const [aba, setAba] = useState('Atributos')
  const [d, setD] = useState({})
  const [erro, setErro] = useState(null)

  useEffect(() => {
    (async () => {
      setD({
        atributos: await api('/api/sistemas/asus/atributos'),
        classes: await api('/api/sistemas/asus/classes'),
        habilidades: await api('/api/sistemas/asus/habilidades'),
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
        <div className="cris-list">
          {(d.classes || []).filter((c) => !c.classePaiCodigo).map((c) => (
            <ClasseCard key={c.codigo} c={c}
              trilhas={(d.classes || []).filter((t) => t.classePaiCodigo === c.codigo)} />
          ))}
        </div>
      )}

      {aba === 'Habilidades' && (
        <HabilidadesView habilidades={d.habilidades || []} classes={d.classes || []} />
      )}

      {aba === 'Perícias' && (
        <div className="pericia-grid">
          {(d.pericias || []).map((p) => <PericiaCard key={p.codigo} p={p} />)}
        </div>
      )}

      {aba === 'Itens' && <ItensView itens={d.itens || []} />}

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
