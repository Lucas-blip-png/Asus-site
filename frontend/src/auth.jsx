import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api } from './api.js'

const AuthContext = createContext(null)
export function useAuth() {
  return useContext(AuthContext)
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [carregando, setCarregando] = useState(true)

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
  }, [])

  function guardarTokens(r) {
    localStorage.setItem('accessToken', r.accessToken)
    localStorage.setItem('refreshToken', r.refreshToken)
    setUser(r.usuario)
    return r.usuario
  }

  useEffect(() => {
    async function carregar() {
      if (!localStorage.getItem('accessToken')) {
        setCarregando(false)
        return
      }
      try {
        setUser(await api('/api/auth/me'))
      } catch {
        try {
          const rt = localStorage.getItem('refreshToken')
          if (!rt) throw new Error('sem refresh')
          guardarTokens(await api('/api/auth/refresh', { method: 'POST', auth: false, body: { refreshToken: rt } }))
        } catch {
          logout()
        }
      } finally {
        setCarregando(false)
      }
    }
    carregar()
  }, [logout])

  const login = (email, senha) =>
    api('/api/auth/login', { method: 'POST', auth: false, body: { email, senha } }).then(guardarTokens)

  const registrar = (nome, email, senha) =>
    api('/api/auth/register', { method: 'POST', auth: false, body: { nome, email, senha } }).then(guardarTokens)

  return (
    <AuthContext.Provider value={{ user, carregando, login, registrar, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
