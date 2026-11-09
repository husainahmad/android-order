package com.harmoni.pos.order.data.model;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    private int id;
    private String orderNo;
    private String customerName;
    private String customerPhone;
    private String remark;
    private String status;
    private double subTotal;
    private double discountTotal;
    private double grandTotal;
    private String createdAt;
    private OrderPayment orderPayment;
    private List<OrderDetail> orderDetails;
    private int storeServiceTypesId;

    public int getId() { return id; }
    public String getOrderNo() { return orderNo == null ? "" : orderNo; }
    public String getCustomerName() { return customerName == null ? "" : customerName; }
    public String getCustomerPhone() { return customerPhone == null ? "" : customerPhone; }
    public String getRemark() { return remark == null ? "" : remark; }
    public String getStatus() { return status == null ? "" : status; }
    public double getSubTotal() { return subTotal; }
    public double getDiscountTotal() { return discountTotal; }
    public double getGrandTotal() { return grandTotal; }
    public String getCreatedAt() { return createdAt == null ? "" : createdAt; }
    public OrderPayment getOrderPayment() { return orderPayment; }
    public List<OrderDetail> getOrderDetails() { return orderDetails; }
    public int getStoreServiceTypesId() { return storeServiceTypesId; }

    public String getServiceTypeName() {
        return storeServiceTypesId == 3 ? "Takeaway" : "Dine In";
    }

    public boolean isConfirmed() { return "CONFIRMED".equalsIgnoreCase(getStatus()); }
    public boolean isPaid() { return "PAID".equalsIgnoreCase(getStatus()); }
    public boolean isVoid() { return "VOID".equalsIgnoreCase(getStatus()); }

    public String getPaymentName() {
        if (orderPayment == null) return "UNPAID";
        switch (orderPayment.getPaymentId()) {
            case 1: return "CASH";
            case 2: return "QR";
            case 3: return "CARD";
            default: return "UNPAID";
        }
    }

    /** Whether any line in the order carries customization choices. */
    public boolean hasCustomizations() {
        if (orderDetails == null) return false;
        for (OrderDetail detail : orderDetails) {
            if (detail != null && detail.hasCustomizations()) return true;
        }
        return false;
    }

    /**
     * A one-line description of what was ordered, with each product's choices in
     * brackets, e.g. "Kopi (Sugar: Less, Milk: Oat) • Americano".
     */
    public String getItemsSummary() {
        if (orderDetails == null) return "";
        StringBuilder sb = new StringBuilder();
        for (OrderDetail detail : orderDetails) {
            if (detail == null) continue;
            String product = detail.getProductName().trim();
            if (product.isEmpty()) continue;
            String choices = detail.getGroupedCustomizationNames();
            String chunk = choices.isEmpty() ? product : product + " (" + choices + ")";
            if (sb.length() > 0) sb.append(" • ");
            sb.append(chunk);
        }
        return sb.toString();
    }
}
