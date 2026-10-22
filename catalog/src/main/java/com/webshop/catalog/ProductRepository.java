package com.webshop.catalog;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> all() {
        return jdbc.sql("""
                SELECT sku, name, description, price_cents, currency, stock
                FROM catalog.product ORDER BY name""")
                .query(Product.class).list();
    }

    public Optional<Product> bySku(String sku) {
        return jdbc.sql("""
                SELECT sku, name, description, price_cents, currency, stock
                FROM catalog.product WHERE sku = :sku""")
                .param("sku", sku)
                .query(Product.class).optional();
    }

    /** Takes stock only if there is enough; false means the basket asked for more than we have. */
    public boolean reserve(String sku, int quantity) {
        return jdbc.sql("""
                UPDATE catalog.product SET stock = stock - :qty, updated_at = now()
                WHERE sku = :sku AND stock >= :qty""")
                .param("sku", sku).param("qty", quantity)
                .update() == 1;
    }
}
