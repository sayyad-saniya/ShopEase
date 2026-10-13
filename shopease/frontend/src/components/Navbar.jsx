import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useCart } from '../context/CartContext.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()
  const { cart } = useCart()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('search') || '')

  function handleSearch(e) {
    e.preventDefault()
    navigate(query ? `/?search=${encodeURIComponent(query)}` : '/')
  }

  return (
    <nav className="navbar navbar-dark shop-navbar sticky-top py-3">
      <div className="container d-flex align-items-center gap-3 flex-wrap">
        <Link to="/" className="navbar-brand mb-0 me-2">ShopEase</Link>

        <form className="d-flex flex-grow-1" style={{ maxWidth: 480 }} onSubmit={handleSearch}>
          <input
            type="search"
            className="form-control"
            placeholder="Search 200+ products…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </form>

        <div className="d-flex align-items-center gap-3 ms-auto">
          <Link to="/cart" className="btn btn-outline-light btn-sm position-relative">
            Cart
            {cart.itemCount > 0 && (
              <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-primary">
                {cart.itemCount}
              </span>
            )}
          </Link>

          {user ? (
            <div className="dropdown">
              <button className="btn btn-light btn-sm dropdown-toggle" data-bs-toggle="dropdown">
                {user.name.split(' ')[0]}
              </button>
              <ul className="dropdown-menu dropdown-menu-end">
                <li><Link className="dropdown-item" to="/orders">My orders</Link></li>
                <li><hr className="dropdown-divider" /></li>
                <li>
                  <button className="dropdown-item" onClick={() => { logout(); navigate('/') }}>
                    Log out
                  </button>
                </li>
              </ul>
            </div>
          ) : (
            <Link to="/login" className="btn btn-primary btn-sm">Log in</Link>
          )}
        </div>
      </div>
    </nav>
  )
}
