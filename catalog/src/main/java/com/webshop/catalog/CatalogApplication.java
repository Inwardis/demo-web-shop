package com.webshop.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * The Catalog container: products, prices, stock. Read-heavy — product pages are cached for 60 s.
 * Owns the {@code catalog} schema; nothing else writes it.
 *
 * @inwardis 41167203-0412-41cc-b638-201b1a4a2041 Catalog
 */
@SpringBootApplication
@EnableCaching
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
