package com.shopease.dao;

import com.shopease.config.DBConnectionManager;
import com.shopease.exception.DataAccessException;
import com.shopease.model.CartItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    public List<CartItem> findByUser(long userId) {
        String sql = "SELECT ci.id, ci.user_id, ci.product_id, ci.quantity, " +
                "p.name AS product_name, p.price AS unit_price, p.image_url, p.stock AS available_stock " +
                "FROM cart_items ci JOIN products p ON p.id = ci.product_id " +
                "WHERE ci.user_id = ? ORDER BY ci.id DESC";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<CartItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load cart", e);
        }
    }

    /** Adds a product to the cart, or increases quantity if it's already in there. */
    public void addOrIncrement(long userId, long productId, int quantity) {
        String sql = "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, productId);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add item to cart", e);
        }
    }

    /** Returns true if a row belonging to that user was updated. */
    public boolean updateQuantity(long userId, long cartItemId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setLong(2, cartItemId);
            stmt.setLong(3, userId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update cart item", e);
        }
    }

    public boolean remove(long userId, long cartItemId) {
        String sql = "DELETE FROM cart_items WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cartItemId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to remove cart item", e);
        }
    }

    public void clear(Connection conn, long userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }

    private CartItem mapRow(ResultSet rs) throws SQLException {
        CartItem item = new CartItem();
        item.setId(rs.getLong("id"));
        item.setUserId(rs.getLong("user_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setProductName(rs.getString("product_name"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setImageUrl(rs.getString("image_url"));
        item.setAvailableStock(rs.getInt("available_stock"));
        return item;
    }
}
