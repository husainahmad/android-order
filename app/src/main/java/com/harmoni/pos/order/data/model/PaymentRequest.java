package com.harmoni.pos.order.data.model;

public class PaymentRequest {
    private int orderId;
    private int paymentId;

    public PaymentRequest(int orderId, int paymentId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
}
