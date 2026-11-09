package com.harmoni.pos.order.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OrderDetail implements Serializable {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private String productName;
    private Integer categoryId;
    private String categoryName;
    /** Kitchen instruction stored on the product line by the order service. */
    private String remark;
    private double amount;
    private double discountAmount;
    private List<OrderDetailSku> orderDetailSkus;
    private List<OrderDetailCustomization> customizations;

    public Integer getId() { return id; }
    public Integer getOrderId() { return orderId; }
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName == null ? "" : productName; }
    public int getCategoryId() { return categoryId == null ? 0 : categoryId; }
    public String getCategoryName() { return categoryName == null ? "" : categoryName; }
    public String getRemark() { return remark == null ? "" : remark; }
    public double getAmount() { return amount; }
    public double getDiscountAmount() { return discountAmount; }
    public List<OrderDetailSku> getOrderDetailSkus() {
        return orderDetailSkus == null ? new ArrayList<>() : orderDetailSkus;
    }
    public List<OrderDetailCustomization> getCustomizations() {
        return customizations == null ? new ArrayList<>() : customizations;
    }

    /** Every chosen option on the line, flattened out of its group. */
    public List<OrderCustomizationLine> getAllCustomizations() {
        List<OrderCustomizationLine> result = new ArrayList<>();
        for (OrderDetailCustomization group : getCustomizations()) {
            for (OrderCustomizationOption option : group.getItems()) {
                result.add(new OrderCustomizationLine(group.getCustomizationNameValue(),
                        option.getOptionNameValue(), option.getPriceValue(),
                        option.getQuantityValue(), option.getAmountValue()));
            }
        }
        return result;
    }

    /**
     * The choices made on one SKU line. Groups with no line (orders written before the
     * link existed) are left to {@link #getUnattachedCustomizations()}.
     */
    public List<OrderCustomizationLine> getCustomizationsForSku(OrderDetailSku sku) {
        List<OrderCustomizationLine> result = new ArrayList<>();
        if (sku == null) return result;
        Integer key = sku.getCustomizationKey();
        if (key == null) return result;
        for (OrderDetailCustomization group : getCustomizations()) {
            if (group.getOrderDetailSkuId() == null || !group.getOrderDetailSkuId().equals(key)) {
                continue;
            }
            for (OrderCustomizationOption option : group.getItems()) {
                result.add(new OrderCustomizationLine(group.getCustomizationNameValue(),
                        option.getOptionNameValue(), option.getPriceValue(),
                        option.getQuantityValue(), option.getAmountValue()));
            }
        }
        return result;
    }

    /** Choices that could not be tied to a line, kept for orders written before the link. */
    public List<OrderCustomizationLine> getUnattachedCustomizations() {
        List<OrderCustomizationLine> result = new ArrayList<>();
        for (OrderDetailCustomization group : getCustomizations()) {
            if (group.getOrderDetailSkuId() != null) continue;
            for (OrderCustomizationOption option : group.getItems()) {
                result.add(new OrderCustomizationLine(group.getCustomizationNameValue(),
                        option.getOptionNameValue(), option.getPriceValue(),
                        option.getQuantityValue(), option.getAmountValue()));
            }
        }
        return result;
    }

    public boolean hasCustomizations() {
        return !getAllCustomizations().isEmpty();
    }

    /** One line per group, e.g. "Sugar: Less • Milk: Oat". */
    public String getGroupedCustomizationNames() {
        StringBuilder sb = new StringBuilder();
        for (OrderCustomizationLine line : getAllCustomizations()) {
            String name = line.getGroupedDisplayName();
            if (name.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" • ");
            sb.append(name);
        }
        return sb.toString();
    }
}
