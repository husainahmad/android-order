package com.harmoni.pos.order.data.model;

import java.util.List;

public class OrderRequest {
    private int storeServiceTypesId;
    private String customerName;
    private int customerId;
    private String customerPhone;
    private String remark;
    private List<OrderDetailRequest> orderDetails;
    private String discount;

    public OrderRequest(int storeServiceTypesId, String customerName, int customerId,
                        String remark, List<OrderDetailRequest> orderDetails, String discount) {
        this(storeServiceTypesId, customerName, customerId, "", remark, orderDetails, discount);
    }

    public OrderRequest(int storeServiceTypesId, String customerName, int customerId,
                        String customerPhone, String remark,
                        List<OrderDetailRequest> orderDetails, String discount) {
        this.storeServiceTypesId = storeServiceTypesId;
        this.customerName = customerName;
        this.customerId = customerId;
        this.customerPhone = customerPhone == null ? "" : customerPhone;
        this.remark = remark;
        this.orderDetails = orderDetails;
        this.discount = discount;
    }

    public int getStoreServiceTypesId() { return storeServiceTypesId; }
    public String getCustomerName() { return customerName; }
    public int getCustomerId() { return customerId; }
    public String getCustomerPhone() { return customerPhone; }
    public String getRemark() { return remark; }
    public List<OrderDetailRequest> getOrderDetails() { return orderDetails; }
    public String getDiscount() { return discount; }
}
