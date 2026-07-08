// Service worker mínimo: torna o app instalável (PWA). Rede sempre em primeiro
// lugar — nada de cache agressivo pra não servir ficha/rolagem velha.
self.addEventListener('install', () => self.skipWaiting())
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()))
self.addEventListener('fetch', (e) => {
  // passthrough: deixa o navegador resolver normalmente
  if (e.request.method !== 'GET') return
})
