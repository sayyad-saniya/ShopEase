package com.shopease.servlet;

import com.shopease.dao.CartDao;
import com.shopease.dao.ProductDao;
import com.shopease.model.CartItem;
import com.shopease.model.Product;
import com.shopease.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET    /api/cart            -> current user's cart items + total
 * POST   /api/cart            -> {productId, quantity} add/increment
 * PUT    /api/cart/{itemId}   -> {quantity} update quantity
 * DELETE /api/cart/{itemId}   -> remove item
 *
 * AuthFilter guarantees a logged-in session before any of these run.
 */
@WebServlet("/api/cart/*")
public class CartServlet extends HttpServlet {

    private final CartDao cartDao = new CartDao();
    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = currentUserId(req);
        List<CartItem> items = cartDao.findByUser(userId);
        JsonUtil.writeJson(resp, 200, cartResponse(items));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = currentUserId(req);
        Map<?, ?> body = JsonUtil.readBody(req, Map.class);

        long productId = asLong(body.get("productId"));
        int quantity = Math.max(1, asInt(body.get("quantity"), 1));

        Product product = productDao.findById(productId);
        if (product == null) {
            JsonUtil.writeError(resp, 404, "That product doesn't exist");
            return;
        }
        if (product.getStock() < quantity) {
            JsonUtil.writeError(resp, 409, "Only " + product.getStock() + " left in stock");
            return;
        }

        cartDao.addOrIncrement(userId, productId, quantity);
        JsonUtil.writeJson(resp, 200, cartResponse(cartDao.findByUser(userId)));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = currentUserId(req);
        long itemId = parseItemId(req);
        if (itemId < 0) {
            JsonUtil.writeError(resp, 400, "Invalid cart item id");
            return;
        }

        Map<?, ?> body = JsonUtil.readBody(req, Map.class);
        int quantity = asInt(body.get("quantity"), -1);
        if (quantity < 1) {
            JsonUtil.writeError(resp, 400, "Quantity must be at least 1 (use DELETE to remove an item)");
            return;
        }

        boolean updated = cartDao.updateQuantity(userId, itemId, quantity);
        if (!updated) {
            JsonUtil.writeError(resp, 404, "Cart item not found");
            return;
        }
        JsonUtil.writeJson(resp, 200, cartResponse(cartDao.findByUser(userId)));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = currentUserId(req);
        long itemId = parseItemId(req);
        if (itemId < 0) {
            JsonUtil.writeError(resp, 400, "Invalid cart item id");
            return;
        }
        boolean removed = cartDao.remove(userId, itemId);
        if (!removed) {
            JsonUtil.writeError(resp, 404, "Cart item not found");
            return;
        }
        JsonUtil.writeJson(resp, 200, cartResponse(cartDao.findByUser(userId)));
    }

    private Map<String, Object> cartResponse(List<CartItem> items) {
        BigDecimal total = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("itemCount", items.stream().mapToInt(CartItem::getQuantity).sum());
        body.put("total", total);
        return body;
    }

    private long parseItemId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) return -1;
        try {
            return Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long currentUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return (Long) session.getAttribute("userId");
    }

    private long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return -1;
        }
    }

    private int asInt(Object o, int fallback) {
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return fallback;
        }
    }
}
