package com.webshop.catalog;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    @Cacheable("products")
    public List<Product> list() {
        return products.all();
    }

    @GetMapping("/{sku}")
    @Cacheable("product")
    public Product one(@PathVariable String sku) {
        return products.bySku(sku).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record Reservation(int quantity) {}

    /** Called by Checkout when an order is placed. Stock is never cached, so this evicts. */
    @PostMapping("/{sku}/reserve")
    @CacheEvict(cacheNames = {"products", "product"}, allEntries = true)
    public void reserve(@PathVariable String sku, @RequestBody Reservation r) {
        if (!products.reserve(sku, r.quantity())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "not enough stock for " + sku);
        }
    }
}
