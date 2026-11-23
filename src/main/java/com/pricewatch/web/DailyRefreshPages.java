package com.pricewatch.web;

import com.pricewatch.catalog.DailyCatalogueRefresh;
import com.pricewatch.catalog.ImportStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
class DailyRefreshPages implements WebMvcConfigurer, HandlerInterceptor {
    private final DailyCatalogueRefresh refresh;

    DailyRefreshPages(DailyCatalogueRefresh refresh) {
        this.refresh = refresh;
    }

    @EventListener(ApplicationReadyEvent.class)
    void startup() {
        refresh.onUse();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView view) {
        if (!"GET".equals(request.getMethod()) || view == null || !"market-desk".equals(view.getViewName())) {
            return;
        }
        refresh.onUse();
        Object scene = view.getModel().get("scene");
        if ("settings".equals(scene) || "data".equals(scene)) {
            var accepted = (ImportStatus) view.getModel().get("importStatus");
            view.addObject("dailyRefreshFailed", refresh.failedToday(accepted));
        }
    }
}
