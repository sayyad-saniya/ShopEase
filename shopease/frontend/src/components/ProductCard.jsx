import { useNavigate } from 'react-router-dom'

export default function ProductCard({ product }) {
  const navigate = useNavigate()

  return (
    <div className="col-6 col-md-4 col-lg-3">
      <div className="card product-card border" onClick={() => navigate(`/product/${product.id}`)}>
        <img src={product.imageUrl} alt={product.name} className="card-img-top" loading="lazy" />
        <div className="card-body d-flex flex-column">
          <span className="badge badge-category align-self-start mb-2">{product.category}</span>
          <h3 className="fs-6 mb-1">{product.name}</h3>
          <div className="mt-auto d-flex align-items-center justify-content-between">
            <span className="price-tag">${Number(product.price).toFixed(2)}</span>
            {product.stock === 0 ? (
              <span className="text-danger small">Out of stock</span>
            ) : product.stock <= 5 ? (
              <span className="text-warning small">Only {product.stock} left</span>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  )
}
