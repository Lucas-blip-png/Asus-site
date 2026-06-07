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
