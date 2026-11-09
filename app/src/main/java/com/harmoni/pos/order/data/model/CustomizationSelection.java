package com.harmoni.pos.order.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The in-progress answer for every customization group of one product, held by the
 * customization picker. {@code selected} maps a customization id to the option ids
 * chosen in that group, with one entry per unit: three extra shots is {@code [55,55,55]}.
 * Unit sums drive quantities and surcharges; the min/max option budget counts distinct
 * options instead, matching the menu service.
 */
public class CustomizationSelection {

    private final Map<Integer, List<Integer>> selected = new HashMap<>();

    public List<Integer> optionIds(int customizationId) {
        List<Integer> ids = selected.get(customizationId);
        return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }

    public int distinctCount(int customizationId) {
        return new HashSet<>(optionIds(customizationId)).size();
    }

    public int count(int optionId, int customizationId) {
        int count = 0;
        for (Integer id : optionIds(customizationId)) {
            if (id != null && id == optionId) count++;
        }
        return count;
    }

    public boolean isSelected(int optionId, int customizationId) {
        return count(optionId, customizationId) > 0;
    }

    public int totalUnits(int customizationId) {
        return optionIds(customizationId).size();
    }

    /** Adds one unit; a single-choice group replaces whatever it held. */
    public void add(int optionId, ProductCustomization group) {
        int id = group.getCustomizationId();
        List<Integer> ids = optionIds(id);
        if (group.allowsMultiple()) {
            if (new HashSet<>(ids).size() >= group.getMaxCount()) return;
            ids.add(optionId);
        } else {
            ids = new ArrayList<>();
            ids.add(optionId);
        }
        selected.put(id, ids);
    }

    public void remove(int optionId, ProductCustomization group) {
        int id = group.getCustomizationId();
        List<Integer> ids = optionIds(id);
        int index = ids.indexOf(optionId);
        if (index < 0) return;
        ids.remove(index);
        if (ids.isEmpty()) selected.remove(id);
        else selected.put(id, ids);
    }

    public void toggle(int optionId, ProductCustomization group) {
        if (isSelected(optionId, group.getCustomizationId())) remove(optionId, group);
        else add(optionId, group);
    }

    /**
     * A tap on an option row. In quantity mode this toggles presence (chosen at quantity
     * one, or cleared entirely); the stepper adjusts the count. Outside quantity mode it
     * is the classic radio/checkbox toggle. A single-choice group stays exclusive.
     */
    public void select(int optionId, ProductCustomization group) {
        if (!group.allowsQuantity()) {
            toggle(optionId, group);
            return;
        }
        int id = group.getCustomizationId();
        if (isSelected(optionId, id)) {
            List<Integer> ids = optionIds(id);
            ids.removeIf(v -> v != null && v == optionId);
            if (ids.isEmpty()) selected.remove(id);
            else selected.put(id, ids);
        } else {
            if (!group.allowsMultiple()) selected.put(id, new ArrayList<>());
            if (distinctCount(id) >= group.getMaxCount()) return;
            List<Integer> ids = optionIds(id);
            ids.add(optionId);
            selected.put(id, ids);
        }
    }

    public void increment(int optionId, ProductCustomization group) {
        if (!group.allowsQuantity()) return;
        int id = group.getCustomizationId();
        if (isSelected(optionId, id)) {
            if (count(optionId, id) >= ProductCustomization.MAX_UNITS_PER_OPTION) return;
            List<Integer> ids = optionIds(id);
            ids.add(optionId);
            selected.put(id, ids);
        } else {
            if (!group.allowsMultiple()) selected.put(id, new ArrayList<>());
            if (distinctCount(id) >= group.getMaxCount()) return;
            List<Integer> ids = optionIds(id);
            ids.add(optionId);
            selected.put(id, ids);
        }
    }

    public void decrement(int optionId, ProductCustomization group) {
        if (!group.allowsQuantity()) return;
        remove(optionId, group);
    }

    public boolean canIncrement(int optionId, ProductCustomization group) {
        if (!group.allowsQuantity()) return false;
        int id = group.getCustomizationId();
        if (isSelected(optionId, id)) {
            return count(optionId, id) < ProductCustomization.MAX_UNITS_PER_OPTION;
        }
        if (!group.allowsMultiple() && !optionIds(id).isEmpty()) return false;
        return distinctCount(id) < group.getMaxCount();
    }

    /**
     * Resolves the selection into cart-ready values, one entry per option carrying how
     * many units were chosen. Groups short of their minimum distinct options are dropped
     * so the operator gets warned rather than sending a line the menu service rejects.
     */
    public List<SelectedCustomization> resolved(List<ProductCustomization> groups) {
        List<SelectedCustomization> result = new ArrayList<>();
        if (groups == null) return result;
        for (ProductCustomization group : groups) {
            int id = group.getCustomizationId();
            List<Integer> ids = optionIds(id);
            if (new HashSet<>(ids).size() < group.getMinCount()) continue;
            for (CustomizationOption option : group.getSortedOptions()) {
                int quantity = count(option.getId(), id);
                if (quantity <= 0) continue;
                result.add(new SelectedCustomization(id, group.getName(), option.getId(),
                        option.getName(), option.getPrice(), quantity));
            }
        }
        return result;
    }

    /** Groups the operator must still resolve before the line can be added. */
    public List<ProductCustomization> unmetRequirements(List<ProductCustomization> groups) {
        List<ProductCustomization> result = new ArrayList<>();
        if (groups == null) return result;
        for (ProductCustomization group : groups) {
            if (distinctCount(group.getCustomizationId()) < group.getMinCount()) {
                result.add(group);
            }
        }
        return result;
    }

    /** Clears every group, used when the picker is reopened for a different line. */
    public void clear() {
        selected.clear();
    }
}
