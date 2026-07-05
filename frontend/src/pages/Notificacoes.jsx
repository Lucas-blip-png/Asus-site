import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { dataHora } from '../format.js'

export default function Notificacoes() {
  const { user } = useAuth()
  const [lista, setLista] = useState([])
  const [erro, setErro] = useState(null)

  const carregar = useCallback(() => {
    if (!user?.id) return
    api(`/api/me/notificacoes?usuarioId=${user.id}`).then(setLista).catch((e) => setErro(e.message))
  }, [user?.id])

  // Ao abrir a tela: carrega e marca tudo como lido, zerando o contador do sino.
  useEffect(() => {
    if (!user?.id) return undefined
    let ativo = true
    api(`/api/me/notificacoes?usuarioId=${user.id}`)
      .then((l) => {
        if (!ativo) return
        setLista(l)
        if (l.some((n) => !n.lida)) {
          api(`/api/me/notificacoes/marcar-todas-lidas?usuarioId=${user.id}`, { method: 'POST' })
            .then(() => {
              if (!ativo) return
              setLista((prev) => prev.map((n) => ({ ...n, lida: true })))
              // avisa o sino (Layout) pra zerar na hora, sem esperar o polling
              window.dispatchEvent(new Event('notificacoes-lidas'))
            })
            .catch(() => {})
        }
      })
      .catch((e) => setErro(e.message))
    return () => { ativo = false }
  }, [user?.id])

  async function marcarLida(id) {
    try { await api(`/api/notificacoes/${id}/lida`, { method: 'POST' }); carregar() }
    catch (e) { setErro(e.message) }
  }
  async function marcarTodas() {
    try {
      await Promise.all(lista.filter((n) => !n.lida).map((n) => api(`/api/notificacoes/${n.id}/lida`, { method: 'POST' })))
      carregar()
    } catch (e) { setErro(e.message) }
  }

  const naoLidas = lista.filter((n) => !n.lida).length

  return (
    <>
      <div className="page-head">
        <h1>Notificações</h1>
        <span className="count-badge"><b>{naoLidas}</b> não lidas</span>
        <div className="spacer" />
        {naoLidas > 0 && <button className="ghost mini" onClick={marcarTodas}>Marcar todas como lidas</button>}
      </div>
      {erro && <p className="error">{erro}</p>}

      {lista.length === 0 && <div className="card muted">Nenhuma notificação por aqui.</div>}
      <div className="lista-vert">
        {lista.map((n) => (
          <div key={n.id} className={`noti-card${n.lida ? ' lida' : ''}`}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="row" style={{ gap: 8 }}>
                <b>{n.titulo}</b>
                {n.tipo && <span className="tag">{n.tipo}</span>}
                <div className="spacer" />
                <span className="muted" style={{ fontSize: '.74rem', whiteSpace: 'nowrap' }}>{dataHora(n.criadaEm)}</span>
              </div>
              <div className="muted" style={{ marginTop: 4 }}>{n.mensagem}</div>
            </div>
            {!n.lida && <button className="ghost mini" onClick={() => marcarLida(n.id)}>Lida</button>}
          </div>
        ))}
      </div>
    </>
  )
}
