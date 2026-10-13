package com.shopease.servlet;

import com.shopease.dao.UserDao;
import com.shopease.model.User;
import com.shopease.util.JsonUtil;
import com.shopease.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POST /api/auth/register  {name, email, password}
 * POST /api/auth/login     {email, password}
 * POST /api/auth/logout
 * GET  /api/auth/me        -> current session's user, or 401
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if ("/register".equals(pathInfo)) {
            register(req, resp);
        } else if ("/login".equals(pathInfo)) {
            login(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            logout(req, resp);
        } else {
            JsonUtil.writeError(resp, 404, "Unknown auth endpoint");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/me".equals(req.getPathInfo())) {
            HttpSession session = req.getSession(false);
            Object userId = session == null ? null : session.getAttribute("userId");
            if (userId == null) {
                JsonUtil.writeError(resp, 401, "Not logged in");
                return;
            }
            User user = userDao.findById((Long) userId);
            JsonUtil.writeJson(resp, 200, toPublicUser(user));
        } else {
            JsonUtil.writeError(resp, 404, "Unknown auth endpoint");
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<?, ?> body = JsonUtil.readBody(req, Map.class);
        String name = str(body.get("name"));
        String email = str(body.get("email"));
        String password = str(body.get("password"));

        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            JsonUtil.writeError(resp, 400, "Name, a valid email, and a password of at least 6 characters are required");
            return;
        }
        if (userDao.findByEmail(email) != null) {
            JsonUtil.writeError(resp, 409, "An account with that email already exists");
            return;
        }

        User user = userDao.create(name, email, PasswordUtil.hash(password));
        startSession(req, user);
        JsonUtil.writeJson(resp, 201, toPublicUser(user));
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<?, ?> body = JsonUtil.readBody(req, Map.class);
        String email = str(body.get("email"));
        String password = str(body.get("password"));

        User user = userDao.findByEmail(email);
        if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
            JsonUtil.writeError(resp, 401, "Incorrect email or password");
            return;
        }
        startSession(req, user);
        JsonUtil.writeJson(resp, 200, toPublicUser(user));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.writeJson(resp, 200, Map.of("message", "Logged out"));
    }

    private void startSession(HttpServletRequest req, User user) {
        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setMaxInactiveInterval(60 * 60 * 24); // 24h
    }

    private Map<String, Object> toPublicUser(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        return map;
    }

    private String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
