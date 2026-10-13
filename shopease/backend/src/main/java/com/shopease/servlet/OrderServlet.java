package com.shopease.servlet;

import com.shopease.dao.CartDao;
import com.shopease.dao.OrderDao;
import com.shopease.exception.ConflictException;
import com.shopease.model.CartItem;
import com.shopease.model.Order;
import com.shopease.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * POST /api/orders        -> checkout the current cart into a new order
 * GET  /api/orders        -> list current user's past orders
 * GET  /api/orders/{id}   -> a single order with its line items
 */
@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();
    private final CartDao cartDao = new CartDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = currentUserId(req);
        List<CartItem> cartItems = cartDao.findByUser(userId);
        try {
            Order order = orderDao.checkout(userId, cartItems);
            JsonUtil.writeJson(resp, 201, order);
        } catch (ConflictException e) {
            JsonUtil.writeError(resp, 409, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = currentUserId(req);
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeJson(resp, 200, orderDao.findByUser(userId));
            return;
        }

        try {
            long orderId = Long.parseLong(pathInfo.substring(1));
            Order order = orderDao.findById(orderId, userId);
            if (order == null) {
                JsonUtil.writeError(resp, 404, "Order not found");
            } else {
                JsonUtil.writeJson(resp, 200, order);
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeError(resp, 400, "Invalid order id");
        }
    }

    private long currentUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return (Long) session.getAttribute("userId");
    }
}
