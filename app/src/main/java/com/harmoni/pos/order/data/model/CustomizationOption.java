package com.harmoni.pos.order.data.model;

import java.util.List;

public class CustomizationOption {
    private Integer id;
    private Integer customizationId;
    private String name;
    private List<CustomizationOptionTierPrice> tierPrices;
    private Boolean isDeleted;

    public int getId() { return id == null ? 0 : id; }
    public int getCustomizationId() { return customizationId == null ? 0 : customizationId; }
    public String getName() { return name == null ? "" : name; }
    public List<CustomizationOptionTierPrice> getTierPrices() { return tierPrices; }
    public boolean isDeleted() { return Boolean.TRUE.equals(isDeleted); }

    /**
     * Client-side estimate of what this option costs, used only so the cart and the
     * confirm popup can show a total before the line is priced. The operator's price
     * tier lives on the store and is not part of any response this app holds, so the
     * lowest live row is used as a stand-in. The order service re-resolves the real
     * amount from the catalogue; only option ids are ever sent.
     */
    public double getPrice() {
        if (tierPrices == null) return 0;
        double min = Double.MAX_VALUE;
        boolean found = false;
        for (CustomizationOptionTierPrice tp : tierPrices) {
            if (tp == null || tp.isDeleted()) continue;
            double p = tp.getPriceValue();
            if (p < min) {
                min = p;
                found = true;
            }
        }
        return found ? min : 0;
    }
}
