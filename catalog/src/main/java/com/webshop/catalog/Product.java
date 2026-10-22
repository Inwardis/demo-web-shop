package com.webshop.catalog;

public record Product(String sku, String name, String description, long priceCents, String currency, int stock) {}
