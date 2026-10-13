package com.shopease.filter;

import com.shopease.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Requires an active login session (see AuthServlet) before letting a request through
 * to the cart or order endpoints. Rejects unauthenticated requests with a JSON 401
 * rather than letting them hit the DAO layer. Registered in web.xml, after
 * ExceptionHandlingFilter, mapped only to /api/cart/* and /api/orders/*.
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to continue");
            return;
        }
        chain.doFilter(request, response);
    }
}
