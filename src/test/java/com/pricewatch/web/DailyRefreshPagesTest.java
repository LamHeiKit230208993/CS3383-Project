package com.pricewatch.web;

import com.pricewatch.catalog.DailyCatalogueRefresh;
import com.pricewatch.catalog.ImportStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DailyRefreshPagesTest {
    private final DailyCatalogueRefresh refresh = mock(DailyCatalogueRefresh.class);
    private final DailyRefreshPages pages = new DailyRefreshPages(refresh);
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Object handler = new Object();

    @Test
    void startupAndNormalPageUseSignalTheCoordinator() {
        pages.startup();
        pages.postHandle(new MockHttpServletRequest("GET", "/list"), response, handler,
                new ModelAndView("market-desk", "scene", "list"));
        pages.postHandle(new MockHttpServletRequest("GET", "/export"), response, handler, null);
        pages.postHandle(new MockHttpServletRequest("POST", "/list"), response, handler,
                new ModelAndView("market-desk"));
        pages.postHandle(new MockHttpServletRequest("GET", "/reminders"), response, handler,
                new ModelAndView("redirect:/alerts"));

        verify(refresh, times(2)).onUse();
        verifyNoMoreInteractions(refresh);
    }

    @Test
    void settingsReceivesThePersistedFailureNotice() {
        var accepted = new ImportStatus("LIVE", "https://example.test/prices", Instant.EPOCH,
                null, "test", 1, 1, 0);
        var view = new ModelAndView("market-desk", "scene", "settings");
        view.addObject("importStatus", accepted);
        when(refresh.failedToday(accepted)).thenReturn(true);

        pages.postHandle(new MockHttpServletRequest("GET", "/settings"), response, handler, view);

        verify(refresh).onUse();
        assertEquals(true, view.getModel().get("dailyRefreshFailed"));
    }
}
