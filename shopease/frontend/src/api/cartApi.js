import apiClient from './apiClient.js'

export async function getCart() {
  const { data } = await apiClient.get('/api/cart')
  return data
}

export async function addToCart(productId, quantity = 1) {
  const { data } = await apiClient.post('/api/cart', { productId, quantity })
  return data
}

export async function updateCartItem(itemId, quantity) {
  const { data } = await apiClient.put(`/api/cart/${itemId}`, { quantity })
  return data
}

export async function removeCartItem(itemId) {
  const { data } = await apiClient.delete(`/api/cart/${itemId}`)
  return data
}
