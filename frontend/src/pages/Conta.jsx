import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, obterOrgId } from '../api.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'

export default function Conta() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const [orgId, setOrgId] = useState(null)
  const [membros, setMembros] = useState([])
  const [novoMembroId, setNovoMembroId] = useState('')
  const [legal, setLegal] = useState(null)
  const [confirmarExclusao, setConfirmarExclusao] = useState(false)
  const [msg, setMsg] = useState(null)
  const [erro, setErro] = useState(null)
  const [resetEmail, setResetEmail] = useState('')
  const [resetSenha, setResetSenha] = useState('')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [minhaNovaSenha, setMinhaNovaSenha] = useState('')

  async function carregar(oid) {
    setMembros(await api(`/api/organizacoes/${oid}/membros`))
  }
  useEffect(() => {
    (async () => { const oid = await obterOrgId(); setOrgId(oid); await carregar(oid) })().catch((e) => setErro(e.message))
  }, [])

  async function addMembro() {
    if (!novoMembroId) return
    setErro(null)
    try {
      // Sem classificacoes: todo mundo entra como membro comum.
      await api(`/api/organizacoes/${orgId}/membros`, {
        method: 'POST',
        body: { usuarioId: Number(novoMembroId) },
      })
      setNovoMembroId('')
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
  // Dono: redefine a senha de um usuário que esqueceu (senhas são hash, não dá pra ver a antiga).
  async function redefinirSenhaUsuario() {
    setErro(null); setMsg(null)
    if (!resetEmail.trim() || resetSenha.length < 6) {
      setErro('Informe o e-mail e uma nova senha de ao menos 6 caracteres.'); return
    }
    try {
      const u = await api('/api/auth/admin/redefinir-senha', {
        method: 'POST', body: { email: resetEmail.trim(), novaSenha: resetSenha },
      })
      setMsg(`Senha de ${u?.email || resetEmail.trim()} redefinida. Passe a nova senha para a pessoa.`)
      setResetEmail(''); setResetSenha('')
    } catch (e) { setErro(e.message) }
  }
  // Qualquer usuário troca a própria senha, provando que sabe a atual.
  async function trocarMinhaSenha() {
    setErro(null); setMsg(null)
    if (!senhaAtual || minhaNovaSenha.length < 6) {
      setErro('Informe a senha atual e uma nova de ao menos 6 caracteres.'); return
    }
    try {
      await api('/api/auth/trocar-senha', { method: 'POST', body: { senhaAtual, novaSenha: minhaNovaSenha } })
      setMsg('Senha alterada com sucesso.')
      setSenhaAtual(''); setMinhaNovaSenha('')
    } catch (e) { setErro(e.message) }
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
        <h2>🔒 Trocar minha senha</h2>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <input type="password" placeholder="Senha atual" style={{ minWidth: 160 }} value={senhaAtual}
            onChange={(e) => setSenhaAtual(e.target.value)} />
          <input type="password" placeholder="Nova senha (mín. 6)" style={{ minWidth: 180 }} value={minhaNovaSenha}
            onChange={(e) => setMinhaNovaSenha(e.target.value)} />
          <button className="mini" onClick={trocarMinhaSenha}>Salvar</button>
        </div>
      </div>

      <div className="card">
        <h2>Membros da minha mesa</h2>
        <p className="muted" style={{ marginTop: -4, fontSize: '.84rem' }}>
          Adicione outras pessoas pelo ID. Todos têm as mesmas permissões.
        </p>
        <div className="lista-vert">
          {membros.map((m) => (
            <div key={m.id} className="row" style={{ gap: 8 }}>
              <span className="dot">{String(m.usuarioId)}</span>
              <span>Usuário #{m.usuarioId}</span>
              <span className="muted" style={{ fontSize: '.74rem' }}>{dataHora(m.entrouEm)}</span>
              <div className="spacer" />
              {m.usuarioId !== user?.id && (
                <button className="ghost mini" onClick={() => removerMembro(m.usuarioId)}>Remover</button>
              )}
            </div>
          ))}
          {!membros.length && <span className="muted">Sem membros ainda.</span>}
        </div>
        <div className="row" style={{ gap: 8, marginTop: 12 }}>
          <input type="number" placeholder="ID do usuário" style={{ maxWidth: 180 }} value={novoMembroId}
            onChange={(e) => setNovoMembroId(e.target.value)} />
          <button className="mini" onClick={addMembro}>Adicionar</button>
        </div>
      </div>

      {user?.dono && (
        <div className="card">
          <h2>🔑 Redefinir senha de um usuário</h2>
          <p className="muted" style={{ marginTop: -4, fontSize: '.84rem' }}>
            Só o dono. Use quando alguém esquecer a senha — define uma nova (a antiga não pode ser vista, é hash).
          </p>
          <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
            <input placeholder="E-mail do usuário" style={{ minWidth: 220 }} value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)} />
            <input type="password" placeholder="Nova senha (mín. 6)" style={{ minWidth: 180 }} value={resetSenha}
              onChange={(e) => setResetSenha(e.target.value)} />
            <button className="mini" onClick={redefinirSenhaUsuario}>Redefinir</button>
          </div>
        </div>
      )}

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
