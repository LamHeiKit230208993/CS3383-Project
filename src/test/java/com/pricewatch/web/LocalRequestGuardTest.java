package com.pricewatch.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalRequestGuardTest {
    @Test
    void loopbackIsAllowedByDefault() throws Exception {
        MockHttpServletResponse response = run(new LocalRequestGuard(""), "127.0.0.1");
        assertEquals(200, response.getStatus());
    }

    @Test
    void configuredLanHostIsAllowed() throws Exception {
        MockHttpServletResponse response = run(new LocalRequestGuard("192.168.1.20"), "192.168.1.20");
        assertEquals(200, response.getStatus());
    }

    @Test
    void unlistedHostIsRejected() throws Exception {
        MockHttpServletResponse response = run(new LocalRequestGuard("192.168.1.20"), "10.0.0.9");
        assertEquals(403, response.getStatus());
    }

    @Test
    void lanHostIsRejectedWhenNotConfigured() throws Exception {
        MockHttpServletResponse response = run(new LocalRequestGuard(""), "192.168.1.20");
        assertEquals(403, response.getStatus());
    }

    private MockHttpServletResponse run(LocalRequestGuard guard, String serverName) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName(serverName);
        MockHttpServletResponse response = new MockHttpServletResponse();
        guard.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
