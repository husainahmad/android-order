package com.harmoni.pos.order.data.model;

import java.util.List;

public class OrderRequest {
    private int storeServiceTypesId;
    private String customerName;
    private int customerId;
    private String remark;
    private List<OrderDetailRequest> orderDetails;
    private String discount;

    public OrderRequest(int storeServiceTypesId, String customerName, int customerId,
                        String remark, List<OrderDetailRequest> orderDetails, String discount) {
        this.storeServiceTypesId = storeServiceTypesId;
        this.customerName = customerName;
        this.customerId = customerId;
        this.remark = remark;
        this.orderDetails = orderDetails;
        this.discount = discount;
    }
}
