package com.harmoni.pos.order.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomizationSelectionTest {

    @Test
    public void singleChoiceReplacesPreviousOption() {
        ProductCustomization group = group(1, "SINGLE", false, 1, 1,
                option(10, "Hot", 0), option(11, "Ice", 0));
        CustomizationSelection selection = new CustomizationSelection();

        selection.select(10, group);
        selection.select(11, group);

        assertFalse(selection.isSelected(10, 1));
        assertTrue(selection.isSelected(11, 1));
        assertEquals(1, selection.distinctCount(1));
    }

    @Test
    public void multipleChoiceHonoursMaxDistinctOptions() {
        ProductCustomization group = group(1, "MULTIPLE", false, 0, 2,
                option(10, "Sugar", 0), option(11, "Milk", 0), option(12, "Shot", 0));
        CustomizationSelection selection = new CustomizationSelection();

        selection.select(10, group);
        selection.select(11, group);
        selection.select(12, group);

        assertTrue(selection.isSelected(10, 1));
        assertTrue(selection.isSelected(11, 1));
        assertFalse(selection.isSelected(12, 1));
    }

    @Test
    public void quantityIncrementsAndResolvesOneEntryPerOption() {
        ProductCustomization group = group(1, "MULTIPLE", true, 0, 1,
                option(10, "Extra Shot", 5000));
        CustomizationSelection selection = new CustomizationSelection();

        selection.select(10, group);
        selection.increment(10, group);
        selection.increment(10, group);

        assertEquals(3, selection.count(10, 1));

        List<SelectedCustomization> resolved = selection.resolved(Collections.singletonList(group));
        assertEquals(1, resolved.size());
        assertEquals(10, resolved.get(0).getOptionId());
        assertEquals(3, resolved.get(0).getQuantity());
        assertEquals("Extra Shot x3", resolved.get(0).getDisplayName());
        assertEquals(15000, resolved.get(0).getPrice() * resolved.get(0).getQuantity(), 0.001);
    }

    @Test
    public void requiredGroupIsUnmetUntilMinimumDistinctOptionsChosen() {
        ProductCustomization group = group(1, "MULTIPLE", false, 2, 3,
                option(10, "A", 0), option(11, "B", 0), option(12, "C", 0));
        CustomizationSelection selection = new CustomizationSelection();

        selection.select(10, group);
        assertEquals(1, selection.unmetRequirements(Collections.singletonList(group)).size());

        selection.select(11, group);
        assertTrue(selection.unmetRequirements(Collections.singletonList(group)).isEmpty());
    }

    @Test
    public void quantityCeilingAndDecrementClampAtZero() {
        ProductCustomization group = group(1, "MULTIPLE", true, 1, 1,
                option(10, "Extra Shot", 0));
        CustomizationSelection selection = new CustomizationSelection();
        selection.select(10, group);

        for (int i = 0; i < ProductCustomization.MAX_UNITS_PER_OPTION + 5; i++) {
            selection.increment(10, group);
        }
        assertEquals(ProductCustomization.MAX_UNITS_PER_OPTION, selection.count(10, 1));
        assertFalse(selection.canIncrement(10, group));

        for (int i = 0; i < ProductCustomization.MAX_UNITS_PER_OPTION; i++) {
            selection.decrement(10, group);
        }
        assertFalse(selection.isSelected(10, 1));
    }

    @Test
    public void cartLineKeyIgnoresOptionOrderAndSeparatesDifferentChoices() {
        List<Integer> a = Arrays.asList(10, 11);
        List<Integer> b = Arrays.asList(11, 10);

        assertEquals(CartItem.lineKey(1, 2, a), CartItem.lineKey(1, 2, b));
        assertFalse(CartItem.lineKey(1, 2, a).equals(CartItem.lineKey(1, 2, Collections.singletonList(10))));
    }

    @Test
    public void cartItemBuildsGroupedCustomizationRequests() {
        List<SelectedCustomization> choices = new ArrayList<>();
        choices.add(new SelectedCustomization(1, "Sugar", 10, "Less Sugar", 0, 1));
        choices.add(new SelectedCustomization(1, "Sugar", 11, "No Sugar", 0, 2));

        CartItem item = new CartItem(1, "Kopi", 1, 2, "Regular", 17000, 1, choices);
        List<OrderCustomizationGroupRequest> requests = item.getCustomizationRequests();

        assertEquals(1, requests.size());
        assertEquals(1, requests.get(0).getCustomizationId());
        assertEquals(2, requests.get(0).getOptions().size());
        assertEquals(10, requests.get(0).getOptions().get(0).getCustomizationOptionId());
        assertEquals(1, requests.get(0).getOptions().get(0).getQuantity());
        assertEquals(11, requests.get(0).getOptions().get(1).getCustomizationOptionId());
        assertEquals(2, requests.get(0).getOptions().get(1).getQuantity());
        assertEquals(17000, item.getLineTotal(), 0.001);
    }

    // --- reflection helpers -------------------------------------------------

    private static ProductCustomization group(int customizationId, String selectionType,
                                              boolean allowQuantity, int min, int max,
                                              CustomizationOption... options) {
        ProductCustomization group = new ProductCustomization();
        set(ProductCustomization.class, group, "customizationId", customizationId);
        set(ProductCustomization.class, group, "name", "Group" + customizationId);
        set(ProductCustomization.class, group, "selectionType", selectionType);
        set(ProductCustomization.class, group, "allowQuantity", allowQuantity);
        set(ProductCustomization.class, group, "required", min > 0);
        set(ProductCustomization.class, group, "minSelection", min);
        set(ProductCustomization.class, group, "maxSelection", max);
        set(ProductCustomization.class, group, "options", Arrays.asList(options));
        return group;
    }

    private static CustomizationOption option(int id, String name, double price) {
        CustomizationOption option = new CustomizationOption();
        set(CustomizationOption.class, option, "id", id);
        set(CustomizationOption.class, option, "name", name);
        CustomizationOptionTierPrice tier = new CustomizationOptionTierPrice();
        set(CustomizationOptionTierPrice.class, tier, "price", price);
        set(CustomizationOption.class, option, "tierPrices", Collections.singletonList(tier));
        return option;
    }

    private static void set(Class<?> clazz, Object target, String field, Object value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
