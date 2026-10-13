import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getOrder } from '../api/orderApi.js'

export default function OrderConfirmationPage() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getOrder(id).then(setOrder).finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="container py-5">Loading…</div>
  if (!order) return <div className="container py-5">Order not found.</div>

  return (
    <div className="container py-5" style={{ maxWidth: 640 }}>
      <div className="text-center mb-4">
        <h1 className="fs-2">Thanks for your order!</h1>
        <p className="text-secondary">Order #{order.id} · {order.status}</p>
      </div>

      <div className="border rounded-3 p-4 bg-white mb-4">
        {order.items.map((item) => (
          <div key={item.id} className="d-flex justify-content-between py-2 border-bottom">
            <span>{item.productName} × {item.quantity}</span>
            <span>${Number(item.unitPrice * item.quantity).toFixed(2)}</span>
          </div>
        ))}
        <div className="d-flex justify-content-between pt-3 fw-semibold fs-5">
          <span>Total</span>
          <span>${Number(order.total).toFixed(2)}</span>
        </div>
      </div>

      <div className="text-center">
        <Link to="/" className="btn btn-primary">Continue shopping</Link>
      </div>
    </div>
  )
}
