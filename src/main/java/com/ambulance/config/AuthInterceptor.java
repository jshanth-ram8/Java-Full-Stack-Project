package com.ambulance.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Very small session-based access control:
 *  - not logged in            -> 401
 *  - wrong role for the path  -> 403
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        String role = session == null ? null : (String) session.getAttribute("role");
        String path = request.getRequestURI();

        if (role == null) {
            return deny(response, 401, "Please log in to continue");
        }
        if (path.startsWith("/api/admin") && !role.equals("ADMIN")) {
            return deny(response, 403, "Admins only");
        }
        if (path.startsWith("/api/driver") && !role.equals("DRIVER")) {
            return deny(response, 403, "Drivers only");
        }
        if (path.startsWith("/api/bookings") && !role.equals("USER")) {
            return deny(response, 403, "Only patients can make bookings");
        }
        return true;
    }

    private boolean deny(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
        return false;
    }
}
