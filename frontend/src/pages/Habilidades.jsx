import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

// Página pública com a descrição de TODAS as habilidades do sistema:
// busca, filtro por classe e por tipo (ativa/passiva). Linkável sem login.
export default function Habilidades() {
  const [habilidades, setHabilidades] = useState([])
  const [classes, setClasses] = useState([])
  const [erro, setErro] = useState(null)
  const [busca, setBusca] = useState('')
  const [classeF, setClasseF] = useState('')
  const [tipoF, setTipoF] = useState('')

  useEffect(() => {
    api('/api/sistemas/asus/habilidades', { auth: false }).then(setHabilidades).catch((e) => setErro(e.message))
    api('/api/sistemas/asus/classes', { auth: false }).then(setClasses).catch(() => {})
  }, [])

  // codigo -> nome bonito da classe (GERAL vira "Geral")
  const nomeClasse = useMemo(() => {
    const m = { GERAL: 'Geral' }
    for (const c of classes) m[c.codigo] = c.nome
    return m
  }, [classes])

  const fmtClasses = (cod) => (cod || '')
    .split(',').filter(Boolean)
    .map((c) => nomeClasse[c.trim()] || c.trim())
    .join(', ')

  const filtradas = useMemo(() => {
    const q = busca.trim().toLowerCase()
    return (habilidades || [])
      .filter((h) => {
        if (classeF && !(h.classeCodigo || '').split(',').map((s) => s.trim()).includes(classeF)) return false
        if (tipoF && h.tipo !== tipoF) return false
        if (q && !(`${h.nome || ''} ${h.efeito || ''} ${h.requisito || ''} ${fmtClasses(h.classeCodigo)}`)
          .toLowerCase().includes(q)) return false
        return true
      })
      .sort((a, b) => (a.nome || '').localeCompare(b.nome || '', 'pt-BR'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [habilidades, busca, classeF, tipoF, nomeClasse])

  return (
    <div className="container" style={{ maxWidth: 900, margin: '0 auto', padding: '26px 18px 60px' }}>
      <h1 className="brand" style={{ textAlign: 'center', justifyContent: 'center' }}>ASUS RPG</h1>
      <div className="page-head" style={{ marginTop: 8 }}>
        <h1>✦ Habilidades</h1>
        <span className="count-badge">{habilidades.length} no total</span>
        <div className="spacer" />
        <Link to="/"><button className="ghost mini">Ir para o app</button></Link>
      </div>

      {erro && <p className="error">{erro}</p>}

      <div className="row" style={{ gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
        <input placeholder="Buscar por nome, efeito, requisito…" value={busca}
          onChange={(e) => setBusca(e.target.value)} style={{ flex: '1 1 240px' }} />
        <select value={classeF} onChange={(e) => setClasseF(e.target.value)}>
          <option value="">Todas as classes</option>
          <option value="GERAL">Geral</option>
          {classes.map((c) => <option key={c.codigo} value={c.codigo}>{c.nome}</option>)}
        </select>
        <select value={tipoF} onChange={(e) => setTipoF(e.target.value)}>
          <option value="">Ativas e passivas</option>
          <option value="ATIVA">Ativas</option>
          <option value="PASSIVA">Passivas</option>
        </select>
      </div>

      <p className="muted" style={{ fontSize: '.8rem', margin: '0 0 10px' }}>
        {filtradas.length} habilidade{filtradas.length === 1 ? '' : 's'} encontrada{filtradas.length === 1 ? '' : 's'}
      </p>

      <div className="lista-vert" style={{ gap: 10 }}>
        {filtradas.map((h) => (
          <div key={h.codigo} className="card" style={{ padding: '14px 16px' }}>
            <div className="row" style={{ gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
              <b style={{ fontSize: '1.02rem' }}>{h.nome}</b>
              <span className="tag">{h.tipo === 'PASSIVA' ? '◈ Passiva' : '◇ Ativa'}</span>
              {h.custo > 0 && <span className="tag">{h.custo} {h.custoTipo || 'PE'}</span>}
              <div className="spacer" />
              <span className="muted" style={{ fontSize: '.76rem' }}>{fmtClasses(h.classeCodigo)}</span>
            </div>
            {h.requisito && (
              <p className="muted" style={{ fontSize: '.78rem', margin: '7px 0 0' }}>
                <b>Requisito:</b> {h.requisito}
              </p>
            )}
            {h.efeito && (
              <p style={{ margin: '8px 0 0', fontSize: '.9rem', lineHeight: 1.55 }}>{h.efeito}</p>
            )}
          </div>
        ))}
        {!filtradas.length && !erro && (
          <div className="muted" style={{ padding: 20, textAlign: 'center' }}>
            {habilidades.length ? 'Nenhuma habilidade com esses filtros.' : 'Carregando…'}
          </div>
        )}
      </div>
    </div>
  )
}
