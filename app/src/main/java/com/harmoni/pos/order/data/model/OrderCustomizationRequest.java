package com.harmoni.pos.order.data.model;

/**
 * One customization choice on a line. Three extra shots is a single entry with
 * {@code quantity = 3}, not three entries. Only the option id travels: the order
 * service resolves the names and prices at the operator's tier.
 */
public class OrderCustomizationRequest {
    private final int customizationOptionId;
    private final int quantity;

    public OrderCustomizationRequest(int customizationOptionId, int quantity) {
        this.customizationOptionId = customizationOptionId;
        this.quantity = Math.max(1, quantity);
    }

    public int getCustomizationOptionId() { return customizationOptionId; }
    public int getQuantity() { return quantity; }
}
