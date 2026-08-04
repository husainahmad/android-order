package com.harmoni.pos.order.data.model;

import java.io.Serializable;

public class OrderDetailSku implements Serializable {
    private String skuName;
    private double quantity;
    private double price;
    private double amount;

    public String getSkuName() { return skuName == null ? "" : skuName; }
    public double getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getAmount() { return amount; }
}
