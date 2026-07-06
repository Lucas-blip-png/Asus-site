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
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = null }
  if (!res.ok) {
    throw new Error((data && data.message) || res.statusText || ('HTTP ' + res.status))
  }
  return data
}

// O backend e multi-tenant (organizacoes); o frontend usa a 1a org do usuario
// (criando uma se nao houver). Mantido em cache na sessao.
let cachedOrgId = null
export async function obterOrgId() {
  if (cachedOrgId) return cachedOrgId
  // Endpoint idempotente: retorna a org pessoal do usuario (cria se nao houver).
  // Estavel entre reloads — antes o frontend criava uma org nova e os dados "sumiam".
  const org = await api('/api/organizacoes/minha')
  cachedOrgId = org.id
  return cachedOrgId
}

export function limparOrgId() { cachedOrgId = null }
