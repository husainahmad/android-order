package com.harmoni.pos.order.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** One customization group chosen on an order line, with the options picked in it. */
public class OrderDetailCustomization implements Serializable {
    private Long id;
    private Integer orderDetailSkuId;
    private Integer customizationId;
    private String customizationName;
    private List<OrderCustomizationOption> options;

    public Long getId() { return id; }
    public Integer getOrderDetailSkuId() { return orderDetailSkuId; }
    public Integer getCustomizationId() { return customizationId; }
    public String getCustomizationNameValue() {
        return customizationName == null ? "" : customizationName.trim();
    }
    public List<OrderCustomizationOption> getOptions() {
        return options == null ? new ArrayList<>() : options;
    }

    public List<OrderCustomizationOption> getItems() {
        List<OrderCustomizationOption> items = new ArrayList<>();
        for (OrderCustomizationOption option : getOptions()) {
            if (option != null && !option.getOptionNameValue().isEmpty()) items.add(option);
        }
        return items;
    }
}
