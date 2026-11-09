package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One customization group assigned to a product, as
 * {@code GET /api/v1/product/{id}/customization} returns it. The selection fields
 * carry the effective values (per-product override already resolved server side).
 */
public class ProductCustomization {

    /** Same ceiling the menu service enforces per option before pricing a line. */
    public static final int MAX_UNITS_PER_OPTION = 50;

    private Integer id;
    private Integer productId;
    private Integer customizationId;
    private String name;
    private String description;
    private String selectionType;
    private Integer brandId;
    private Boolean required;
    private Boolean allowQuantity;
    private Integer minSelection;
    private Integer maxSelection;
    private Integer sortOrder;
    private List<CustomizationOption> options;

    public int getId() { return id == null ? 0 : id; }
    public int getProductId() { return productId == null ? 0 : productId; }
    public int getCustomizationId() { return customizationId == null ? 0 : customizationId; }
    public String getName() { return name == null ? "" : name; }
    public String getDescription() { return description == null ? "" : description; }
    public String getSelectionType() { return selectionType == null ? "" : selectionType; }
    public Integer getBrandId() { return brandId; }
    public Boolean getRequired() { return required; }
    public Boolean getAllowQuantity() { return allowQuantity; }
    public Integer getMinSelection() { return minSelection; }
    public Integer getMaxSelection() { return maxSelection; }
    public Integer getSortOrder() { return sortOrder; }
    public List<CustomizationOption> getOptions() { return options; }

    public boolean isRequired() { return Boolean.TRUE.equals(required); }
    public boolean allowsMultiple() { return "MULTIPLE".equalsIgnoreCase(getSelectionType()); }
    public boolean allowsQuantity() { return Boolean.TRUE.equals(allowQuantity); }

    public int getMinCount() {
        if (minSelection != null) return Math.max(0, minSelection);
        return isRequired() ? 1 : 0;
    }

    public int getMaxCount() {
        int declared;
        if (maxSelection != null) {
            declared = maxSelection;
        } else {
            declared = allowsMultiple() ? getSortedOptions().size() : 1;
        }
        return allowsMultiple() ? Math.max(1, declared) : 1;
    }

    public List<CustomizationOption> getSortedOptions() {
        List<CustomizationOption> result = new ArrayList<>();
        if (options == null) return result;
        for (CustomizationOption option : options) {
            if (option != null && !option.isDeleted()) result.add(option);
        }
        return result;
    }
}
