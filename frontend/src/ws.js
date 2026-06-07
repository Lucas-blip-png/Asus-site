import { Client } from '@stomp/stompjs'

// Cria um cliente STOMP sobre WebSocket nativo (Fase 6).
export function criarStomp() {
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const url = import.meta.env.VITE_WS_URL || `${proto}://${window.location.host}/ws`
  return new Client({ brokerURL: url, reconnectDelay: 3000 })
}

/**
 * Assina um topico. Retorna uma funcao de cleanup que desconecta.
 * onMessage recebe o corpo ja parseado (JSON).
 */
export function inscrever(destino, onMessage) {
  const client = criarStomp()
  client.onConnect = () => {
    client.subscribe(destino, (frame) => {
      try {
        onMessage(JSON.parse(frame.body))
      } catch {
        onMessage(frame.body)
      }
    })
  }
  client.activate()
  return () => client.deactivate()
}
