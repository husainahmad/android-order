package com.harmoni.pos.order.data.model;

public class CartItem {
    private final int productId;
    private final String productName;
    private final int categoryId;
    private final int skuId;
    private final String skuName;
    private final double price;
    private int quantity;

    public CartItem(int productId, String productName, int categoryId, int skuId,
                    String skuName, double price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.skuId = skuId;
        this.skuName = skuName;
        this.price = price;
        this.quantity = quantity;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCategoryId() { return categoryId; }
    public int getSkuId() { return skuId; }
    public String getSkuName() { return skuName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void addQuantity(int delta) {
        quantity = Math.max(0, quantity + delta);
    }

    public double getLineTotal() {
        return price * quantity;
    }
}
