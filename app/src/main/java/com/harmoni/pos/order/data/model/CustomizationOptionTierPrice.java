package com.harmoni.pos.order.data.model;

public class CustomizationOptionTierPrice {
    private Integer id;
    private Integer customizationOptionId;
    private Integer tierId;
    private Double price;
    private Boolean deleted;

    public Integer getId() { return id; }
    public Integer getCustomizationOptionId() { return customizationOptionId; }
    public Integer getTierId() { return tierId; }
    public double getPrice() { return price == null ? 0 : price; }
    public boolean isDeleted() { return Boolean.TRUE.equals(deleted); }

    /** A live price row, used when the operator's tier cannot be matched client side. */
    public double getPriceValue() {
        return price == null || price <= 0 ? 0 : price;
    }
}
