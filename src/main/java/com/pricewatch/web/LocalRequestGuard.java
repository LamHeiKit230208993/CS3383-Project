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

/** Reject cross-site writes and requests from public or unrelated hostnames. */
@Component
public class LocalRequestGuard extends OncePerRequestFilter {
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String serverName = request.getServerName().toLowerCase(java.util.Locale.ROOT);
        if (!LOCAL_HOSTS.contains(serverName) && !isPrivateLanAddress(serverName)) {
            response.sendError(403, "Use localhost or a private-network address to open Market Desk.");
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

    private boolean isPrivateLanAddress(String host) {
        String[] octets = host.split("\\.");
        if (octets.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(octets[0]);
            int second = Integer.parseInt(octets[1]);
            int third = Integer.parseInt(octets[2]);
            int fourth = Integer.parseInt(octets[3]);
            if (first < 0 || first > 255 || second < 0 || second > 255
                    || third < 0 || third > 255 || fourth < 0 || fourth > 255) {
                return false;
            }
            return first == 10
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 169 && second == 254)
                    || (first == 100 && second >= 64 && second <= 127);
        } catch (NumberFormatException exception) {
            return false;
        }
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
