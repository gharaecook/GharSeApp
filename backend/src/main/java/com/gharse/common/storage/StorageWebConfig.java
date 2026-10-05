package com.gharse.common.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves locally stored uploads (local dev / single node). */
@Configuration
public class StorageWebConfig implements WebMvcConfigurer {

    private final LocalStorageService local;

    public StorageWebConfig(LocalStorageService local) {
        this.local = local;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(local.root().toUri().toString());
    }
}
