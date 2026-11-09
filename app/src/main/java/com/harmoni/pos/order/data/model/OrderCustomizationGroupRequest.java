package com.harmoni.pos.order.data.model;

import java.util.List;

/**
 * One customization group on a line, matching the order service shape:
 * {@code { "customizationId": 5, "options": [{ "customizationOptionId": 11, "quantity": 1 }] }}.
 */
public class OrderCustomizationGroupRequest {
    private final int customizationId;
    private final List<OrderCustomizationRequest> options;

    public OrderCustomizationGroupRequest(int customizationId,
                                          List<OrderCustomizationRequest> options) {
        this.customizationId = customizationId;
        this.options = options;
    }

    public int getCustomizationId() { return customizationId; }
    public List<OrderCustomizationRequest> getOptions() { return options; }
}
