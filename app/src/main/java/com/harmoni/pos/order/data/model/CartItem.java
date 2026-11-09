package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CartItem {
    private final int productId;
    private final String productName;
    private final int categoryId;
    private final int skuId;
    private final String skuName;
    private final double price;
    private int quantity;
    private final List<SelectedCustomization> customizations;
    /** Kitchen instruction for this line; null when the cashier left it blank. */
    private String remark;

    public CartItem(int productId, String productName, int categoryId, int skuId,
                    String skuName, double price, int quantity) {
        this(productId, productName, categoryId, skuId, skuName, price, quantity,
                new ArrayList<>(), null);
    }

    public CartItem(int productId, String productName, int categoryId, int skuId,
                    String skuName, double price, int quantity,
                    List<SelectedCustomization> customizations) {
        this(productId, productName, categoryId, skuId, skuName, price, quantity,
                customizations, null);
    }

    public CartItem(int productId, String productName, int categoryId, int skuId,
                    String skuName, double price, int quantity,
                    List<SelectedCustomization> customizations, String remark) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.skuId = skuId;
        this.skuName = skuName;
        this.price = price;
        this.quantity = quantity;
        this.customizations = customizations == null ? new ArrayList<>() : new ArrayList<>(customizations);
        this.remark = normalizeRemark(remark);
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCategoryId() { return categoryId; }
    public int getSkuId() { return skuId; }
    public String getSkuName() { return skuName; }
    /** Base SKU price, before customization surcharges. */
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public List<SelectedCustomization> getCustomizations() { return customizations; }
    /** Kitchen instruction for this line, or an empty string when none. */
    public String getRemark() { return remark == null ? "" : remark; }

    /**
     * Identity of the line, not of the product. Two lines of the same SKU with
     * different customization choices are different lines and must never merge.
     */
    public String getLineKey() {
        return lineKey(productId, skuId, getCustomizationOptionIds(), remark);
    }

    public static String lineKey(int productId, int skuId, List<Integer> optionIds) {
        return lineKey(productId, skuId, optionIds, null);
    }

    /**
     * Identity of the line. The note is part of the key: two lines of the same
     * SKU and options with different kitchen instructions must not merge, or one
     * instruction would silently replace the other.
     */
    public static String lineKey(int productId, int skuId, List<Integer> optionIds, String remark) {
        List<Integer> sorted = new ArrayList<>(optionIds);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        sb.append(productId).append('-').append(skuId);
        for (Integer id : sorted) sb.append('-').append(id);
        String note = normalizeRemark(remark);
        if (note != null && !note.isEmpty()) sb.append("|r:").append(note);
        return sb.toString();
    }

    private static String normalizeRemark(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Sorted, with each choice repeated once per unit, so order of picking does not matter. */
    public List<Integer> getCustomizationOptionIds() {
        List<Integer> ids = new ArrayList<>();
        for (SelectedCustomization c : customizations) {
            ids.addAll(c.getRepeatedOptionIds());
        }
        Collections.sort(ids);
        return ids;
    }

    /**
     * The chosen options in the shape the order request carries them: grouped by
     * customization, one entry per option carrying how many units were chosen.
     */
    public List<OrderCustomizationGroupRequest> getCustomizationRequests() {
        Map<Integer, List<SelectedCustomization>> grouped = new LinkedHashMap<>();
        for (SelectedCustomization c : customizations) {
            grouped.computeIfAbsent(c.getCustomizationId(), k -> new ArrayList<>()).add(c);
        }
        List<Integer> groupIds = new ArrayList<>(grouped.keySet());
        Collections.sort(groupIds);

        List<OrderCustomizationGroupRequest> result = new ArrayList<>();
        for (Integer groupId : groupIds) {
            List<SelectedCustomization> choices = grouped.get(groupId);
            if (choices == null) continue;
            Collections.sort(choices, (a, b) -> Integer.compare(a.getOptionId(), b.getOptionId()));
            List<OrderCustomizationRequest> options = new ArrayList<>();
            for (SelectedCustomization c : choices) {
                options.add(new OrderCustomizationRequest(c.getOptionId(),
                        Math.max(1, c.getQuantity())));
            }
            result.add(new OrderCustomizationGroupRequest(groupId, options));
        }
        return result;
    }

    /** Display-only estimate of the per-unit customization surcharge. */
    public double getUnitSurcharge() {
        double total = 0;
        for (SelectedCustomization c : customizations) {
            total += c.getPrice() * Math.max(1, c.getQuantity());
        }
        return total;
    }

    /** Unit price including the estimated customization surcharge. */
    public double getUnitPrice() {
        return price + getUnitSurcharge();
    }

    public double getLineTotal() {
        return getUnitPrice() * quantity;
    }

    /** One line per group, e.g. "Sugar: Less Sugar x2 • Milk: Oat". */
    public String getCustomizationSummary() {
        if (customizations.isEmpty()) return "";
        Map<Integer, List<SelectedCustomization>> grouped = new LinkedHashMap<>();
        for (SelectedCustomization c : customizations) {
            grouped.computeIfAbsent(c.getCustomizationId(), k -> new ArrayList<>()).add(c);
        }
        List<Integer> groupIds = new ArrayList<>(grouped.keySet());
        Collections.sort(groupIds);
        StringBuilder sb = new StringBuilder();
        for (Integer groupId : groupIds) {
            List<SelectedCustomization> choices = grouped.get(groupId);
            if (choices == null) continue;
            Collections.sort(choices, (a, b) -> Integer.compare(a.getOptionId(), b.getOptionId()));
            for (SelectedCustomization c : choices) {
                if (sb.length() > 0) sb.append(" • ");
                if (!c.getCustomizationName().isEmpty()) {
                    sb.append(c.getCustomizationName()).append(": ");
                }
                sb.append(c.getDisplayName());
            }
        }
        return sb.toString();
    }

    public void addQuantity(int delta) {
        quantity = Math.max(0, quantity + delta);
    }

    public CartItem copy() {
        return new CartItem(productId, productName, categoryId, skuId,
                skuName, price, quantity, customizations, remark);
    }
}
