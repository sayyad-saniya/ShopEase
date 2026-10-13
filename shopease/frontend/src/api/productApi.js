import apiClient from './apiClient.js'

export async function searchProducts({ search = '', category = '', page = 1, limit = 20 } = {}) {
  const { data } = await apiClient.get('/api/products', {
    params: { search: search || undefined, category: category || undefined, page, limit }
  })
  return data
}

export async function getProduct(id) {
  const { data } = await apiClient.get(`/api/products/${id}`)
  return data
}

export async function getCategories() {
  const { data } = await apiClient.get('/api/products/categories')
  return data
}
