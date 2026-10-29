package com.pricewatch.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

/** Reject browser writes from other sites and requests using non-local hostnames. */
@Component
public class LocalRequestGuard extends OncePerRequestFilter {
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!LOCAL_HOSTS.contains(request.getServerName().toLowerCase(java.util.Locale.ROOT))) {
            response.sendError(403, "Use localhost or the loopback address to open Market Desk.");
            return;
        }
        if (!READ_METHODS.contains(request.getMethod())) {
            String origin = request.getHeader("Origin");
            if ("cross-site".equalsIgnoreCase(request.getHeader("Sec-Fetch-Site"))
                    || (origin != null && !sameOrigin(origin, request))) {
                response.sendError(403, "Open Market Desk directly before changing local data.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean sameOrigin(String value, HttpServletRequest request) {
        try {
            URI origin = URI.create(value);
            int port = origin.getPort() == -1 ? ("https".equalsIgnoreCase(origin.getScheme()) ? 443 : 80) : origin.getPort();
            return request.getScheme().equalsIgnoreCase(origin.getScheme())
                    && request.getServerName().equalsIgnoreCase(origin.getHost())
                    && request.getServerPort() == port
                    && origin.getUserInfo() == null
                    && (origin.getPath() == null || origin.getPath().isEmpty());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
