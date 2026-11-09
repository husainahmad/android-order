package com.harmoni.pos.order.data.model;

/**
 * A single chosen option flattened out of its group, carrying the group name so a
 * receipt can say "Sugar: Less Sugar". This is what the detail screen and printers render.
 */
public class OrderCustomizationLine {
    private final String customizationName;
    private final String optionName;
    private final double price;
    private final int quantity;
    private final double amount;

    public OrderCustomizationLine(String customizationName, String optionName,
                                  double price, int quantity, double amount) {
        this.customizationName = customizationName == null ? "" : customizationName.trim();
        this.optionName = optionName == null ? "" : optionName.trim();
        this.price = price;
        this.quantity = Math.max(1, quantity);
        this.amount = amount;
    }

    public String getCustomizationName() { return customizationName; }
    public String getOptionName() { return optionName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public double getAmount() { return amount; }

    public String getDisplayName() {
        return quantity > 1 ? optionName + " x" + quantity : optionName;
    }

    /** Full line with its group, e.g. "Sugar: Less Sugar x2". */
    public String getGroupedDisplayName() {
        if (customizationName.isEmpty() || optionName.isEmpty()) return getDisplayName();
        return customizationName + ": " + getDisplayName();
    }
}
