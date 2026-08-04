package com.harmoni.pos.order.data.model;

import java.util.List;

public class OrderDetailRequest {
    private int productId;
    private String productName;
    private int categoryId;
    private List<OrderDetailSkuRequest> orderDetailSkus;

    public OrderDetailRequest(int productId, String productName, int categoryId,
                              List<OrderDetailSkuRequest> orderDetailSkus) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.orderDetailSkus = orderDetailSkus;
    }
}
