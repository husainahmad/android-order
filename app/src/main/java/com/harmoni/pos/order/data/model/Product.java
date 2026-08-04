package com.harmoni.pos.order.data.model;

import java.util.List;

public class Product {
    private int id;
    private String name;
    private int categoryId;
    private List<Sku> skus;
    private ProductImage productImage;

    public int getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public int getCategoryId() { return categoryId; }
    public List<Sku> getSkus() { return skus; }
    public ProductImage getProductImage() { return productImage; }
}
