package com.digitalmoneyhouse.accountservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger accessLog = LoggerFactory.getLogger("AUDIT_ACCESS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String userId = extractUserId(request);

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            accessLog.info("method={} path={} userId={} status={} durationMs={}",
                    method, path, userId, status, durationMs);
        }
    }

    private String extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return "anonymous";
        }
        try {
            String token = header.substring("Bearer ".length());
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "unknown";
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return payload.split("\"sub\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }
}