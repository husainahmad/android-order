package com.harmoni.pos.order.data.model;

public class OrderDetailSkuRequest {
    private int skuId;
    private String skuName;
    private int quantity;

    public OrderDetailSkuRequest(int skuId, String skuName, int quantity) {
        this.skuId = skuId;
        this.skuName = skuName;
        this.quantity = quantity;
    }
}
