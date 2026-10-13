import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import * as cartApi from '../api/cartApi.js'
import { useAuth } from './AuthContext.jsx'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const { user } = useAuth()
  const [cart, setCart] = useState({ items: [], itemCount: 0, total: 0 })

  const refresh = useCallback(async () => {
    if (!user) {
      setCart({ items: [], itemCount: 0, total: 0 })
      return
    }
    try {
      const data = await cartApi.getCart()
      setCart(data)
    } catch {
      setCart({ items: [], itemCount: 0, total: 0 })
    }
  }, [user])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function addItem(productId, quantity = 1) {
    const data = await cartApi.addToCart(productId, quantity)
    setCart(data)
  }

  async function updateItem(itemId, quantity) {
    const data = await cartApi.updateCartItem(itemId, quantity)
    setCart(data)
  }

  async function removeItem(itemId) {
    const data = await cartApi.removeCartItem(itemId)
    setCart(data)
  }

  return (
    <CartContext.Provider value={{ cart, refresh, addItem, updateItem, removeItem }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  return useContext(CartContext)
}
