import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { searchProducts, getCategories } from '../api/productApi.js'
import ProductCard from '../components/ProductCard.jsx'
import CategoryFilter from '../components/CategoryFilter.jsx'

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const search = searchParams.get('search') || ''
  const category = searchParams.get('category') || ''
  const page = Number(searchParams.get('page') || 1)

  const [categories, setCategories] = useState([])
  const [result, setResult] = useState({ products: [], totalCount: 0, totalPages: 1, tookMs: 0 })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    setLoading(true)
    searchProducts({ search, category, page, limit: 20 })
      .then(setResult)
      .finally(() => setLoading(false))
  }, [search, category, page])

  function updateParams(next) {
    const params = { search, category, page: 1, ...next }
    const cleaned = Object.fromEntries(Object.entries(params).filter(([, v]) => v))
    setSearchParams(cleaned)
  }

  return (
    <div className="container py-4">
      <div className="d-flex align-items-baseline justify-content-between flex-wrap mb-2">
        <h1 className="fs-3 mb-0">
          {search ? `Results for "${search}"` : category ? category : 'All products'}
        </h1>
        {!loading && (
          <span className="search-took">
            {result.totalCount} result{result.totalCount === 1 ? '' : 's'} · {result.tookMs}ms
          </span>
        )}
      </div>

      <CategoryFilter
        categories={categories}
        active={category}
        onSelect={(cat) => updateParams({ category: cat })}
      />

      {loading ? (
        <div className="py-5 text-center text-secondary">Loading products…</div>
      ) : result.products.length === 0 ? (
        <div className="py-5 text-center text-secondary">No products matched. Try a different search.</div>
      ) : (
        <>
          <div className="row g-3">
            {result.products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          {result.totalPages > 1 && (
            <nav className="d-flex justify-content-center mt-4">
              <ul className="pagination">
                {Array.from({ length: result.totalPages }, (_, i) => i + 1).map((p) => (
                  <li key={p} className={`page-item ${p === page ? 'active' : ''}`}>
                    <button className="page-link" onClick={() => updateParams({ page: p })}>
                      {p}
                    </button>
                  </li>
                ))}
              </ul>
            </nav>
          )}
        </>
      )}
    </div>
  )
}
