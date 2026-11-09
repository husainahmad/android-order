package com.harmoni.pos.order.data.model;

import java.io.Serializable;

public class OrderDetailSku implements Serializable {
    /** The order_detail_skus row id. This is what customization groups reference. */
    private Integer id;
    /** The catalogue SKU id. */
    private Integer skuId;
    private String skuName;
    private double quantity;
    private double price;
    private double amount;
    private double discountAmount;

    public Integer getId() { return id; }
    public Integer getSkuId() { return skuId; }
    public String getSkuName() { return skuName == null ? "" : skuName; }
    public double getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getAmount() { return amount; }
    public double getDiscountAmount() { return discountAmount; }

    /** The id a customization group can reference, or null when the response carried neither. */
    public Integer getCustomizationKey() { return id != null ? id : skuId; }
}
