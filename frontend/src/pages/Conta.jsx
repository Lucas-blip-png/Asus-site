import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'

const PLANOS = ['FREE', 'PRO', 'MESTRE', 'GUILD']
const PAPEIS = ['DONO', 'ADMIN', 'MESTRE', 'JOGADOR', 'OBSERVADOR']

function bytes(n) {
  if (n == null) return '—'
  if (n >= 1 << 30) return (n / (1 << 30)).toFixed(1) + ' GB'
  if (n >= 1 << 20) return (n / (1 << 20)).toFixed(0) + ' MB'
  return (n / 1024).toFixed(0) + ' KB'
}

export default function Conta() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const [orgId, setOrgId] = useState(null)
  const [assinatura, setAssinatura] = useState(null)
  const [limites, setLimites] = useState(null)
  const [membros, setMembros] = useState([])
  const [novoMembro, setNovoMembro] = useState({ usuarioId: '', papel: 'JOGADOR' })
  const [legal, setLegal] = useState(null)
  const [confirmarExclusao, setConfirmarExclusao] = useState(false)
  const [msg, setMsg] = useState(null)
  const [erro, setErro] = useState(null)

  async function carregar(oid) {
    setAssinatura(await api(`/api/organizacoes/${oid}/assinatura`))
    setLimites(await api(`/api/organizacoes/${oid}/limites`))
    setMembros(await api(`/api/organizacoes/${oid}/membros`))
  }
  useEffect(() => {
    (async () => { const oid = await obterOrgId(); setOrgId(oid); await carregar(oid) })().catch((e) => setErro(e.message))
  }, [])

  async function trocarPlano(plano) {
    setErro(null); setMsg(null)
    try {
      setAssinatura(await api(`/api/organizacoes/${orgId}/assinatura`, { method: 'PUT', body: { plano } }))
      setLimites(await api(`/api/organizacoes/${orgId}/limites`))
      setMsg(`Plano atualizado para ${plano}.`)
    } catch (e) { setErro(e.message) }
  }
  async function addMembro() {
    if (!novoMembro.usuarioId) return
    setErro(null)
    try {
      await api(`/api/organizacoes/${orgId}/membros`, {
        method: 'POST',
        body: { usuarioId: Number(novoMembro.usuarioId), papel: novoMembro.papel },
      })
      setNovoMembro({ usuarioId: '', papel: 'JOGADOR' })
      setMembros(await api(`/api/organizacoes/${orgId}/membros`))
    } catch (e) { setErro(e.message) }
  }
  async function removerMembro(usuarioId) {
    try {
      await api(`/api/organizacoes/${orgId}/membros/${usuarioId}`, { method: 'DELETE' })
      setMembros(await api(`/api/organizacoes/${orgId}/membros`))
    } catch (e) { setErro(e.message) }
  }

  async function exportarDados() {
    setErro(null)
    try {
      const dados = await api(`/api/me/export-data?usuarioId=${user.id}`)
      const blob = new Blob([JSON.stringify(dados, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `meus-dados-${user.id}.json`; a.click()
      URL.revokeObjectURL(url)
    } catch (e) { setErro(e.message) }
  }
  async function registrarConsentimento(tipo) {
    setErro(null); setMsg(null)
    try {
      await api(`/api/me/consentimentos?usuarioId=${user.id}`, {
        method: 'POST', body: { tipo, versaoDocumento: '1.0', aceito: true },
      })
      setMsg(`Consentimento de ${tipo} registrado.`)
    } catch (e) { setErro(e.message) }
  }
  async function abrirLegal(doc) {
    setErro(null)
    try { setLegal(await api(`/api/legal/${doc}`)) } catch (e) { setErro(e.message) }
  }
  async function excluirConta() {
    setErro(null)
    try {
      await api(`/api/me/delete-account?usuarioId=${user.id}`, { method: 'DELETE' })
      logout(); nav('/login')
    } catch (e) { setErro(e.message); setConfirmarExclusao(false) }
  }

  return (
    <>
      <div className="page-head"><h1>Minha conta</h1></div>
      {erro && <p className="error">{erro}</p>}
      {msg && <p className="ok">{msg}</p>}

      <div className="card">
        <h2>Dados</h2>
        <div className="kv"><b>Nome</b><span>{user?.nome}</span></div>
        <div className="kv"><b>E-mail</b><span>{user?.email || '—'}</span></div>
        <div className="kv"><b>ID</b><span>#{user?.id}</span></div>
      </div>

      <div className="card">
        <h2>Plano & limites</h2>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          {PLANOS.map((p) => (
            <button key={p} className={assinatura?.plano === p ? '' : 'ghost'} onClick={() => trocarPlano(p)}>{p}</button>
          ))}
        </div>
        {assinatura && (
          <div className="muted" style={{ marginTop: 8 }}>
            Status: <b>{assinatura.status}</b>
            {assinatura.inicio ? ` · desde ${dataHora(assinatura.inicio)}` : ''}
          </div>
        )}
        {limites && (
          <div className="row" style={{ gap: 6, flexWrap: 'wrap', marginTop: 10 }}>
            <span className="tag">Personagens {limites.maxPersonagens}</span>
            <span className="tag">Campanhas {limites.maxCampanhas}</span>
            <span className="tag">Jogadores/camp. {limites.maxJogadoresPorCampanha}</span>
            <span className="tag">Histórico {limites.historicoRolagensDias}d</span>
            <span className="tag">Assets {bytes(limites.assetsBytesMax)}</span>
            <span className="tag">{limites.overlayObs ? 'Overlay OBS ✓' : 'Sem Overlay'}</span>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Membros da organização</h2>
        <div className="lista-vert">
          {membros.map((m) => (
            <div key={m.id} className="row" style={{ gap: 8 }}>
              <span className="dot">{String(m.usuarioId)}</span>
              <span>Usuário #{m.usuarioId}</span>
              <span className="tag">{m.papel}</span>
              <span className="muted" style={{ fontSize: '.74rem' }}>{dataHora(m.entrouEm)}</span>
              <div className="spacer" />
              <button className="ghost mini" onClick={() => removerMembro(m.usuarioId)}>Remover</button>
            </div>
          ))}
          {!membros.length && <span className="muted">Sem membros listados.</span>}
        </div>
        <div className="row" style={{ gap: 8, marginTop: 12 }}>
          <input type="number" placeholder="ID do usuário" style={{ maxWidth: 150 }} value={novoMembro.usuarioId}
            onChange={(e) => setNovoMembro((s) => ({ ...s, usuarioId: e.target.value }))} />
          <select value={novoMembro.papel} onChange={(e) => setNovoMembro((s) => ({ ...s, papel: e.target.value }))}>
            {PAPEIS.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
          <button className="mini" onClick={addMembro}>Adicionar</button>
        </div>
      </div>

      <div className="card">
        <h2>Privacidade & dados (LGPD)</h2>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <button className="ghost" onClick={exportarDados}>⬇ Exportar meus dados</button>
          <button className="ghost" onClick={() => registrarConsentimento('TERMOS')}>Aceitar termos</button>
          <button className="ghost" onClick={() => registrarConsentimento('PRIVACIDADE')}>Aceitar privacidade</button>
          <button className="ghost" onClick={() => abrirLegal('termos')}>Ver termos</button>
          <button className="ghost" onClick={() => abrirLegal('privacidade')}>Ver privacidade</button>
        </div>
        <div className="row" style={{ marginTop: 14 }}>
          <button className="danger" onClick={() => setConfirmarExclusao(true)}>Excluir minha conta</button>
        </div>
      </div>

      {legal && (
        <div className="modal" onClick={() => setLegal(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 640 }}>
            <h2>{legal.documento} · v{legal.versao}</h2>
            <div className="legal-conteudo">{legal.conteudo}</div>
            <button style={{ marginTop: 12 }} onClick={() => setLegal(null)}>Fechar</button>
          </div>
        </div>
      )}

      {confirmarExclusao && (
        <div className="modal" onClick={() => setConfirmarExclusao(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>Excluir conta?</h2>
            <p className="muted">Esta ação remove/anonimiza seus dados e não pode ser desfeita.</p>
            <div className="row" style={{ marginTop: 12, gap: 8 }}>
              <button className="danger" onClick={excluirConta}>Sim, excluir</button>
              <button className="ghost" onClick={() => setConfirmarExclusao(false)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
