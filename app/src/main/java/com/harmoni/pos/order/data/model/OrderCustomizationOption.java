package com.harmoni.pos.order.data.model;

import java.io.Serializable;

/** One chosen option on an order line, as the order service stores it. */
public class OrderCustomizationOption implements Serializable {
    private Long id;
    private Integer customizationOptionsId;
    private String customizationOptionName;
    private Integer quantity;
    private Double price;
    private Double amount;
    /** Some payloads name the field without the plural "s". */
    private String name;

    public int getOptionId() {
        if (customizationOptionsId != null) return customizationOptionsId;
        return id == null ? 0 : id.intValue();
    }

    public String getOptionNameValue() {
        String direct = customizationOptionName == null ? "" : customizationOptionName.trim();
        if (!direct.isEmpty()) return direct;
        return name == null ? "" : name.trim();
    }

    public int getQuantityValue() {
        return quantity == null ? 1 : Math.max(1, quantity);
    }

    public double getPriceValue() { return price == null ? 0 : price; }

    public double getAmountValue() {
        if (amount != null) return amount;
        return getPriceValue() * getQuantityValue();
    }

    /** The option as a receipt should name it; the count appears only above one. */
    public String getDisplayName() {
        String n = getOptionNameValue();
        return getQuantityValue() > 1 ? n + " x" + getQuantityValue() : n;
    }
}
