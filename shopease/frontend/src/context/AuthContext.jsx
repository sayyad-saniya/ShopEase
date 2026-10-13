import { createContext, useContext, useEffect, useState } from 'react'
import * as authApi from '../api/authApi.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    authApi.fetchCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function doLogin(email, password) {
    const u = await authApi.login(email, password)
    setUser(u)
    return u
  }

  async function doRegister(name, email, password) {
    const u = await authApi.register(name, email, password)
    setUser(u)
    return u
  }

  async function doLogout() {
    await authApi.logout()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login: doLogin, register: doRegister, logout: doLogout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
