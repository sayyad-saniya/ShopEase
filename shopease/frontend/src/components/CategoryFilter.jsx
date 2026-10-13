export default function CategoryFilter({ categories, active, onSelect }) {
  return (
    <div className="d-flex gap-2 overflow-auto pb-2 mb-3">
      <button
        className={`category-pill ${!active ? 'active' : ''}`}
        onClick={() => onSelect('')}
      >
        All
      </button>
      {categories.map((cat) => (
        <button
          key={cat}
          className={`category-pill ${active === cat ? 'active' : ''}`}
          onClick={() => onSelect(cat)}
        >
          {cat}
        </button>
      ))}
    </div>
  )
}
