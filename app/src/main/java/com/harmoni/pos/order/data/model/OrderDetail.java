package com.harmoni.pos.order.data.model;

import java.io.Serializable;
import java.util.List;

public class OrderDetail implements Serializable {
    private String productName;
    private int categoryId;
    private List<OrderDetailSku> orderDetailSkus;

    public String getProductName() { return productName == null ? "" : productName; }
    public int getCategoryId() { return categoryId; }
    public List<OrderDetailSku> getOrderDetailSkus() { return orderDetailSkus; }
}
