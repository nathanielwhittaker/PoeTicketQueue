package com.poeticketqueue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Only fall back to the SPA shell for route-like paths (no file extension in the
                        // last segment) — e.g. "/group". A missing asset ("/foo.png", "/bar.js") has an
                        // extension and should 404 normally, not silently return index.html.
                        String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                        if (lastSegment.contains(".")) {
                            return null;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
