import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext.jsx'
import { checkout } from '../api/orderApi.js'

export default function CartPage() {
  const { cart, updateItem, removeItem, refresh } = useCart()
  const navigate = useNavigate()
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState(null)

  async function handleCheckout() {
    setPlacing(true)
    setError(null)
    try {
      const order = await checkout()
      await refresh()
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setError(err?.response?.data?.message || 'Checkout failed. Please try again.')
      setPlacing(false)
    }
  }

  if (cart.items.length === 0) {
    return (
      <div className="container py-5 text-center">
        <h1 className="fs-3">Your cart is empty</h1>
        <p className="text-secondary">Browse the catalog and add something you like.</p>
      </div>
    )
  }

  return (
    <div className="container py-4">
      <h1 className="fs-3 mb-4">Your cart</h1>
      <div className="row g-4">
        <div className="col-lg-8">
          {cart.items.map((item) => (
            <div key={item.id} className="d-flex align-items-center gap-3 border rounded-3 p-3 mb-3 bg-white">
              <img src={item.imageUrl} alt={item.productName} width={72} height={72} style={{ objectFit: 'cover', borderRadius: 8 }} />
              <div className="flex-grow-1">
                <div className="fw-semibold">{item.productName}</div>
                <div className="text-secondary small">${Number(item.unitPrice).toFixed(2)} each</div>
              </div>
              <input
                type="number"
                min={1}
                max={item.availableStock}
                className="form-control"
                style={{ width: 80 }}
                value={item.quantity}
                onChange={(e) => updateItem(item.id, Math.max(1, Math.min(item.availableStock, Number(e.target.value))))}
              />
              <div className="price-tag" style={{ width: 90, textAlign: 'right' }}>
                ${Number(item.unitPrice * item.quantity).toFixed(2)}
              </div>
              <button className="btn btn-outline-danger btn-sm" onClick={() => removeItem(item.id)}>
                Remove
              </button>
            </div>
          ))}
        </div>

        <div className="col-lg-4">
          <div className="border rounded-3 p-4 bg-white">
            <div className="d-flex justify-content-between mb-2">
              <span className="text-secondary">Items</span>
              <span>{cart.itemCount}</span>
            </div>
            <div className="d-flex justify-content-between fs-5 fw-semibold mb-3">
              <span>Total</span>
              <span>${Number(cart.total).toFixed(2)}</span>
            </div>
            {error && <p className="text-danger small">{error}</p>}
            <button className="btn btn-primary w-100" onClick={handleCheckout} disabled={placing}>
              {placing ? 'Placing order…' : 'Checkout'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
