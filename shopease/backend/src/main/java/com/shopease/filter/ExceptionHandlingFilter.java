package com.shopease.filter;

import com.shopease.exception.ConflictException;
import com.shopease.exception.DataAccessException;
import com.shopease.exception.NotFoundException;
import com.shopease.exception.ValidationException;
import com.shopease.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandlingFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(ExceptionHandlingFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } catch (ValidationException e) {
            JsonUtil.writeError(resp, 400, e.getMessage());
        } catch (NotFoundException e) {
            JsonUtil.writeError(resp, 404, e.getMessage());
        } catch (ConflictException e) {
            JsonUtil.writeError(resp, 409, e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Database error", e);
            JsonUtil.writeError(resp, 500, "A database error occurred. Please try again.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error", e);
            JsonUtil.writeError(resp, 500, "Something went wrong. Please try again.");
        }
    }
}
