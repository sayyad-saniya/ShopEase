import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOrders } from '../api/orderApi.js'

export default function OrdersPage() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getOrders().then(setOrders).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="container py-5">Loading…</div>

  if (orders.length === 0) {
    return (
      <div className="container py-5 text-center">
        <h1 className="fs-3">No orders yet</h1>
        <p className="text-secondary">Once you check out, your orders will show up here.</p>
      </div>
    )
  }

  return (
    <div className="container py-4">
      <h1 className="fs-3 mb-4">Your orders</h1>
      {orders.map((order) => (
        <Link
          key={order.id}
          to={`/orders/${order.id}`}
          className="d-flex justify-content-between align-items-center border rounded-3 p-3 mb-3 bg-white text-decoration-none text-body"
        >
          <div>
            <div className="fw-semibold">Order #{order.id}</div>
            <div className="text-secondary small">
              {new Date(order.createdAt).toLocaleDateString()} · {order.items.length} item{order.items.length === 1 ? '' : 's'}
            </div>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span className="badge bg-light text-dark border">{order.status}</span>
            <span className="price-tag">${Number(order.total).toFixed(2)}</span>
          </div>
        </Link>
      ))}
    </div>
  )
}
