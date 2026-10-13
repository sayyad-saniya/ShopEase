import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getProduct } from '../api/productApi.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useCart } from '../context/CartContext.jsx'

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { addItem } = useCart()

  const [product, setProduct] = useState(null)
  const [quantity, setQuantity] = useState(1)
  const [status, setStatus] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getProduct(id)
      .then(setProduct)
      .catch(() => setProduct(null))
      .finally(() => setLoading(false))
  }, [id])

  async function handleAddToCart() {
    if (!user) {
      navigate('/login')
      return
    }
    setStatus('adding')
    try {
      await addItem(product.id, quantity)
      setStatus('added')
    } catch (err) {
      setStatus(err?.response?.data?.message || 'Could not add to cart')
    }
  }

  if (loading) return <div className="container py-5">Loading…</div>
  if (!product) return <div className="container py-5">Product not found.</div>

  return (
    <div className="container py-4">
      <div className="row g-4">
        <div className="col-md-5">
          <img src={product.imageUrl} alt={product.name} className="img-fluid rounded-4 border" />
        </div>
        <div className="col-md-7">
          <span className="badge badge-category mb-2">{product.category}</span>
          <h1 className="fs-2">{product.name}</h1>
          <p className="text-secondary">{product.description}</p>
          <div className="price-tag fs-3 mb-3">${Number(product.price).toFixed(2)}</div>

          {product.stock === 0 ? (
            <p className="text-danger fw-semibold">Out of stock</p>
          ) : (
            <>
              <p className="text-secondary small">{product.stock} in stock</p>
              <div className="d-flex align-items-center gap-2 mb-3">
                <input
                  type="number"
                  min={1}
                  max={product.stock}
                  className="form-control"
                  style={{ width: 90 }}
                  value={quantity}
                  onChange={(e) => setQuantity(Math.max(1, Math.min(product.stock, Number(e.target.value))))}
                />
                <button className="btn btn-primary" onClick={handleAddToCart} disabled={status === 'adding'}>
                  {status === 'adding' ? 'Adding…' : 'Add to cart'}
                </button>
              </div>
              {status === 'added' && <p className="text-success small">Added to your cart.</p>}
              {status && status !== 'adding' && status !== 'added' && (
                <p className="text-danger small">{status}</p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
