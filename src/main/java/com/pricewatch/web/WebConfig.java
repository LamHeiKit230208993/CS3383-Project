package com.pricewatch.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the Vite/React SPA built into {@code frontend/dist/} at the root URL
 * and falls back to {@code index.html} for any path that does not match a
 * static file. API routes under {@code /api/**} are not affected — they are
 * handled by {@code @RestController} mappings.
 *
 * <p>Run {@code npm run build} in {@code frontend/} before starting Spring, or
 * use {@code npm run dev} on :5173 with the {@code /api} proxy for local dev.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String SPA_DIST = "file:./frontend/dist/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/**")
            .addResourceLocations(SPA_DIST, "classpath:/static/")
            .setCachePeriod(0)
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    // Root URL: Spring passes an empty path; serve index.html.
                    if (resourcePath == null || resourcePath.isEmpty()) {
                        return indexIfPresent(location);
                    }

                    Resource requested = location.createRelative(resourcePath);
                    if (requested.exists() && requested.isReadable()) {
                        return requested;
                    }
                    // SPA fallback: serve index.html for unknown paths so React
                    // Router (or our state-based routing) can handle the route.
                    return indexIfPresent(location);
                }

                private Resource indexIfPresent(Resource location) throws IOException {
                    Resource index = location.createRelative("index.html");
                    return (index.exists() && index.isReadable()) ? index : null;
                }
            });
    }
}
