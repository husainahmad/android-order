package com.harmoni.pos.order.data.model;

public class Sku {
    private int id;
    private String name;
    private TierPrice tierPrice;

    public int getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public TierPrice getTierPrice() { return tierPrice; }
    public double getPrice() { return tierPrice != null ? tierPrice.getPrice() : 0; }
}
