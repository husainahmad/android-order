package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One customization choice the cashier made, kept on the cart line so the cart,
 * the summary and the confirm popup can all name it. Only {@code optionId} travels
 * to the server; the order service resolves the price and names itself.
 */
public class SelectedCustomization {
    private final int customizationId;
    private final String customizationName;
    private final int optionId;
    private final String optionName;
    private final double price;
    private int quantity;

    public SelectedCustomization(int customizationId, String customizationName,
                                 int optionId, String optionName, double price, int quantity) {
        this.customizationId = customizationId;
        this.customizationName = customizationName == null ? "" : customizationName;
        this.optionId = optionId;
        this.optionName = optionName == null ? "" : optionName;
        this.price = price;
        this.quantity = Math.max(1, quantity);
    }

    public int getCustomizationId() { return customizationId; }
    public String getCustomizationName() { return customizationName; }
    public int getOptionId() { return optionId; }
    public String getOptionName() { return optionName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    /** The choice as a ticket should name it; the count only appears above one. */
    public String getDisplayName() {
        return quantity > 1 ? optionName + " x" + quantity : optionName;
    }

    /** The option repeated once per unit, matching how a group's selections are counted. */
    public List<Integer> getRepeatedOptionIds() {
        return new ArrayList<>(Collections.nCopies(Math.max(1, quantity), optionId));
    }
}
