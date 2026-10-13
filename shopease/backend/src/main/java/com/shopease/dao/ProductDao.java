package com.shopease.dao;

import com.shopease.config.DBConnectionManager;
import com.shopease.exception.DataAccessException;
import com.shopease.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All queries use PreparedStatement with bound parameters (never string-concatenated
 * SQL) to prevent SQL injection. Search relies on the FULLTEXT index defined on
 * products(name, description) in schema.sql, which is what keeps lookups fast across
 * the catalog instead of a full table scan per keystroke.
 */
public class ProductDao {

    public static class SearchResult {
        public final List<Product> products;
        public final int totalCount;

        public SearchResult(List<Product> products, int totalCount) {
            this.products = products;
            this.totalCount = totalCount;
        }
    }

    public SearchResult search(String query, String category, int page, int limit) {
        int offset = Math.max(0, (page - 1)) * limit;
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (hasQuery) {
            // Boolean mode fulltext search: fast even as the catalog grows, and tolerant
            // of partial/multi-word queries. Falls back to a LIKE scan for 1-2 char queries
            // where MySQL's fulltext parser would otherwise ignore short words.
            if (query.trim().length() >= 3) {
                where.append(" AND MATCH(name, description) AGAINST (? IN BOOLEAN MODE) ");
                params.add(likeToBooleanQuery(query));
            } else {
                where.append(" AND name LIKE ? ");
                params.add("%" + query.trim() + "%");
            }
        }
        if (hasCategory) {
            where.append(" AND category = ? ");
            params.add(category);
        }

        String countSql = "SELECT COUNT(*) FROM products" + where;
        String dataSql = "SELECT id, name, description, price, image_url, category, stock, created_at " +
                "FROM products" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";

        try (Connection conn = DBConnectionManager.getConnection()) {
            int totalCount;
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                bindParams(countStmt, params);
                try (ResultSet rs = countStmt.executeQuery()) {
                    rs.next();
                    totalCount = rs.getInt(1);
                }
            }

            List<Product> products = new ArrayList<>();
            try (PreparedStatement dataStmt = conn.prepareStatement(dataSql)) {
                bindParams(dataStmt, params);
                dataStmt.setInt(params.size() + 1, limit);
                dataStmt.setInt(params.size() + 2, offset);
                try (ResultSet rs = dataStmt.executeQuery()) {
                    while (rs.next()) {
                        products.add(mapRow(rs));
                    }
                }
            }
            return new SearchResult(products, totalCount);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to search products", e);
        }
    }

    public Product findById(long id) {
        String sql = "SELECT id, name, description, price, image_url, category, stock, created_at " +
                "FROM products WHERE id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load product " + id, e);
        }
    }

    public List<String> listCategories() {
        String sql = "SELECT DISTINCT category FROM products ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load categories", e);
        }
    }

    /** Reduces stock for a purchased product; returns false if there isn't enough left. */
    public boolean decrementStock(Connection conn, long productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setLong(2, productId);
            stmt.setInt(3, quantity);
            return stmt.executeUpdate() == 1;
        }
    }

    private void bindParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }

    private String likeToBooleanQuery(String raw) {
        // Turn "wireless mouse" into "+wireless* +mouse*" so every term is required
        // and prefix-matched, which behaves closer to what shoppers expect from search.
        StringBuilder sb = new StringBuilder();
        for (String term : raw.trim().split("\\s+")) {
            String cleaned = term.replaceAll("[+\\-<>()~*\"@]", "");
            if (!cleaned.isEmpty()) {
                sb.append("+").append(cleaned).append("* ");
            }
        }
        return sb.toString().trim();
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setImageUrl(rs.getString("image_url"));
        p.setCategory(rs.getString("category"));
        p.setStock(rs.getInt("stock"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            p.setCreatedAt(ts.toInstant());
        }
        return p;
    }
}
