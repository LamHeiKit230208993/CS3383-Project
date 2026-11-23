package com.pricewatch.web;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalRequestGuardTest {
    private final LocalRequestGuard guard = new LocalRequestGuard();

    @ParameterizedTest
    @CsvSource({
            "GET, 127.0.0.1, http, 8080, https://example.test, cross-site, true",
            "GET, 192.168.0.2, http, 8080, , , false",
            "HEAD, ::1, http, 8080, , , true",
            "OPTIONS, LOCALHOST, http, 8080, , , true",
            "POST, 127.0.0.1, http, 8080, , , true",
            "POST, 127.0.0.1, http, 8080, http://127.0.0.1:8080, cross-site, false",
            "POST, 127.0.0.1, http, 8080, http://127.0.0.1:8080, same-origin, true",
            "POST, localhost, http, 80, http://localhost, , true",
            "POST, localhost, https, 443, https://localhost, , true",
            "POST, localhost, http, 8080, https://localhost:8080, , false",
            "POST, localhost, http, 8080, http://example.test:8080, , false",
            "POST, localhost, http, 8080, http://localhost:8081, , false",
            "POST, localhost, http, 8080, http://user@localhost:8080, , false",
            "POST, localhost, http, 8080, http://localhost:8080/form, , false",
            "POST, localhost, http, 8080, not a URI, , false"
    })
    void appliesTheLocalHostAndSameOriginRules(String method, String host, String scheme, int port, String origin,
                                               String fetchSite, boolean allowed) throws IOException, ServletException {
        var request = new MockHttpServletRequest(method, "/list");
        request.setServerName(host);
        request.setScheme(scheme);
        request.setServerPort(port);
        if (origin != null) request.addHeader("Origin", origin);
        if (fetchSite != null) request.addHeader("Sec-Fetch-Site", fetchSite);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        guard.doFilter(request, response, chain);

        assertEquals(allowed ? 200 : 403, response.getStatus());
        if (allowed) assertNotNull(chain.getRequest());
        else assertNull(chain.getRequest());
    }
}
