package com.gharse.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Throttles credential endpoints per client address. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;

    public RateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();
        boolean credentialEndpoint = uri.equals("/api/v1/auth/login") || uri.equals("/api/v1/auth/register")
                || uri.equals("/api/v1/auth/refresh") || uri.equals("/api/v1/auth/forgot-password")
                || uri.equals("/api/v1/auth/reset-password");
        return !credentialEndpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (limiter.tryAcquire(req.getRemoteAddr() + ":auth")) {
            chain.doFilter(req, res);
            return;
        }
        res.setStatus(429);
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests, slow down\"}");
    }
}
