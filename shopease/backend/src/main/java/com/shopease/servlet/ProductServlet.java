package com.shopease.servlet;

import com.shopease.dao.ProductDao;
import com.shopease.model.Product;
import com.shopease.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/products                -> paginated + searchable product list
 * GET /api/products/categories     -> distinct category names, for filter chips
 * GET /api/products/{id}           -> a single product
 */
@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            listProducts(req, resp);
            return;
        }

        if (pathInfo.equals("/categories")) {
            JsonUtil.writeJson(resp, 200, productDao.listCategories());
            return;
        }

        String idPart = pathInfo.substring(1);
        try {
            long id = Long.parseLong(idPart);
            Product product = productDao.findById(id);
            if (product == null) {
                JsonUtil.writeError(resp, 404, "Product " + id + " was not found");
            } else {
                JsonUtil.writeJson(resp, 200, product);
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeError(resp, 400, "Invalid product id: " + idPart);
        }
    }

    private void listProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String search = req.getParameter("search");
        String category = req.getParameter("category");
        int page = parseIntOrDefault(req.getParameter("page"), 1);
        int limit = Math.min(100, parseIntOrDefault(req.getParameter("limit"), 20));

        long start = System.nanoTime();
        ProductDao.SearchResult result = productDao.search(search, category, page, limit);
        double tookMs = (System.nanoTime() - start) / 1_000_000.0;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("products", result.products);
        body.put("totalCount", result.totalCount);
        body.put("page", page);
        body.put("limit", limit);
        body.put("totalPages", (int) Math.ceil(result.totalCount / (double) limit));
        body.put("tookMs", Math.round(tookMs * 100.0) / 100.0);
        JsonUtil.writeJson(resp, 200, body);
    }

    private int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
