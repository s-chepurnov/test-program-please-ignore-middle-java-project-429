package io.hexlet.flightbooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                // classpath — локально: make build кладёт фронт в resources/public,
                // file — Docker/Render: фронт лежит рядом с jar, в /app/public/.
                .addResourceLocations("classpath:/public/", "file:./public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {

                        var requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        var index = location.createRelative("index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
