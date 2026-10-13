package com.shopease.dao;

import com.shopease.config.DBConnectionManager;
import com.shopease.exception.ConflictException;
import com.shopease.exception.DataAccessException;
import com.shopease.model.CartItem;
import com.shopease.model.Order;
import com.shopease.model.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private final ProductDao productDao = new ProductDao();
    private final CartDao cartDao = new CartDao();

    /**
     * Converts the user's current cart into an order inside a single transaction:
     * validates + reserves stock for every line, inserts the order and its items,
     * then empties the cart. Any failure (e.g. a product sold out mid-checkout)
     * rolls the whole thing back so stock and orders never drift out of sync.
     */
    public Order checkout(long userId, List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            throw new ConflictException("Your cart is empty");
        }

        Connection conn = null;
        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false);

            BigDecimal total = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                boolean reserved = productDao.decrementStock(conn, item.getProductId(), item.getQuantity());
                if (!reserved) {
                    throw new ConflictException("\"" + item.getProductName() + "\" no longer has enough stock");
                }
                total = total.add(item.getLineTotal());
            }

            long orderId = insertOrder(conn, userId, total);
            for (CartItem item : cartItems) {
                insertOrderItem(conn, orderId, item);
            }
            cartDao.clear(conn, userId);

            conn.commit();
            return findById(orderId, userId);
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Checkout failed", e);
        } catch (ConflictException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    public List<Order> findByUser(long userId) {
        String sql = "SELECT id, user_id, total, status, created_at FROM orders WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
                for (Order order : orders) {
                    order.setItems(findItems(conn, order.getId()));
                }
                return orders;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load orders", e);
        }
    }

    public Order findById(long orderId, long userId) {
        String sql = "SELECT id, user_id, total, status, created_at FROM orders WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Order order = mapOrder(rs);
                order.setItems(findItems(conn, order.getId()));
                return order;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load order " + orderId, e);
        }
    }

    private long insertOrder(Connection conn, long userId, BigDecimal total) throws SQLException {
        String sql = "INSERT INTO orders (user_id, total, status) VALUES (?, ?, 'PLACED')";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId);
            stmt.setBigDecimal(2, total);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertOrderItem(Connection conn, long orderId, CartItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            stmt.setLong(2, item.getProductId());
            stmt.setString(3, item.getProductName());
            stmt.setInt(4, item.getQuantity());
            stmt.setBigDecimal(5, item.getUnitPrice());
            stmt.executeUpdate();
        }
    }

    private List<OrderItem> findItems(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT id, order_id, product_id, product_name, quantity, unit_price " +
                "FROM order_items WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (rs.next()) {
                    OrderItem oi = new OrderItem();
                    oi.setId(rs.getLong("id"));
                    oi.setOrderId(rs.getLong("order_id"));
                    oi.setProductId(rs.getLong("product_id"));
                    oi.setProductName(rs.getString("product_name"));
                    oi.setQuantity(rs.getInt("quantity"));
                    oi.setUnitPrice(rs.getBigDecimal("unit_price"));
                    items.add(oi);
                }
                return items;
            }
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setUserId(rs.getLong("user_id"));
        order.setTotal(rs.getBigDecimal("total"));
        order.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            order.setCreatedAt(ts.toInstant());
        }
        return order;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // best effort - the original exception is what matters to the caller
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
