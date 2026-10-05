package com.gharse.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds a request id (also returned as X-Request-Id) to the logging context and logs one line per request:
 * method, path, status, duration and user id. Never logs bodies, headers or tokens.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        res.setHeader("X-Request-Id", requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            String userId = MDC.get("userId") == null ? "-" : MDC.get("userId"); // set by JwtAuthFilter
            if (!req.getRequestURI().startsWith("/actuator")) {
                log.info("http method={} path={} status={} durationMs={} userId={}", req.getMethod(),
                        req.getRequestURI(), res.getStatus(), ms, userId);
            }
            MDC.clear();
        }
    }
}
