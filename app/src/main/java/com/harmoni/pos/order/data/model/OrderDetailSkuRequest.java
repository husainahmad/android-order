package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailSkuRequest {
    private int skuId;
    private String skuName;
    private int quantity;
    private List<OrderCustomizationGroupRequest> customizations;

    public OrderDetailSkuRequest(int skuId, String skuName, int quantity) {
        this(skuId, skuName, quantity, new ArrayList<>());
    }

    public OrderDetailSkuRequest(int skuId, String skuName, int quantity,
                                 List<OrderCustomizationGroupRequest> customizations) {
        this.skuId = skuId;
        this.skuName = skuName;
        this.quantity = quantity;
        this.customizations = customizations == null ? new ArrayList<>() : customizations;
    }

    public int getSkuId() { return skuId; }
    public String getSkuName() { return skuName; }
    public int getQuantity() { return quantity; }
    public List<OrderCustomizationGroupRequest> getCustomizations() { return customizations; }
}
