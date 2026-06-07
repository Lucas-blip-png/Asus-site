// Cliente HTTP minimo com Bearer token (Fase 7).
const BASE = import.meta.env.VITE_API_BASE || ''

let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

export function getToken() {
  return localStorage.getItem('accessToken')
}

export async function api(path, { method = 'GET', body, auth = true, headers = {} } = {}) {
  const h = { ...headers }
  const isForm = body instanceof FormData
  if (body !== undefined && !isForm) h['Content-Type'] = 'application/json'
  if (auth && getToken()) h['Authorization'] = 'Bearer ' + getToken()

  const res = await fetch(BASE + path, {
    method,
    headers: h,
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  })

  if (res.status === 401 && onUnauthorized) onUnauthorized()

  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error((data && data.message) || res.statusText)
  }
  return data
}

// O backend e multi-tenant (organizacoes); o frontend usa a 1a org do usuario
// (criando uma se nao houver). Mantido em cache na sessao.
let cachedOrgId = null
export async function obterOrgId() {
  if (cachedOrgId) return cachedOrgId
  const orgs = await api('/api/organizacoes')
  if (orgs && orgs.length > 0) {
    cachedOrgId = orgs[0].id
  } else {
    const nova = await api('/api/organizacoes', {
      method: 'POST',
      body: { nome: 'Minha Mesa', slug: 'mesa-' + Math.random().toString(36).slice(2, 8) },
    })
    cachedOrgId = nova.id
  }
  return cachedOrgId
}
