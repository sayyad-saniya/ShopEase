import apiClient from './apiClient.js'

export async function checkout() {
  const { data } = await apiClient.post('/api/orders')
  return data
}

export async function getOrders() {
  const { data } = await apiClient.get('/api/orders')
  return data
}

export async function getOrder(id) {
  const { data } = await apiClient.get(`/api/orders/${id}`)
  return data
}
