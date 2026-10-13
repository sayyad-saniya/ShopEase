import apiClient from './apiClient.js'

export async function register(name, email, password) {
  const { data } = await apiClient.post('/api/auth/register', { name, email, password })
  return data
}

export async function login(email, password) {
  const { data } = await apiClient.post('/api/auth/login', { email, password })
  return data
}

export async function logout() {
  const { data } = await apiClient.post('/api/auth/logout')
  return data
}

export async function fetchCurrentUser() {
  const { data } = await apiClient.get('/api/auth/me')
  return data
}
