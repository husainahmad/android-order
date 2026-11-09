package com.harmoni.pos.order.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.harmoni.pos.order.util.PhoneUtils;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;

public class ModelHelpersTest {

    @Test
    public void customerDisplayNameFallsBackToPhoneThenId() {
        assertEquals("Budi", customer(5, "Budi", "0812").getDisplayName());
        assertEquals("0812", customer(5, "", "0812").getDisplayName());
        assertEquals("Customer #5", customer(5, "", "").getDisplayName());
    }

    @Test
    public void phoneHelpersNormaliseAndValidate() {
        assertEquals("0812345", PhoneUtils.digits("0812-345"));
        assertFalse(PhoneUtils.isSearchable("12"));
        assertTrue(PhoneUtils.isSearchable("123"));
        assertTrue(PhoneUtils.isValid("0812 3456"));
        assertFalse(PhoneUtils.isValid("abc"));
    }

    @Test
    public void orderItemsSummaryIncludesCustomizations() {
        OrderCustomizationOption option = new OrderCustomizationOption();
        set(OrderCustomizationOption.class, option, "customizationOptionName", "Less Sugar");
        set(OrderCustomizationOption.class, option, "quantity", 1);

        OrderDetailCustomization group = new OrderDetailCustomization();
        set(OrderDetailCustomization.class, group, "customizationName", "Sugar");
        set(OrderDetailCustomization.class, group, "options", Collections.singletonList(option));

        OrderDetail detail = new OrderDetail();
        set(OrderDetail.class, detail, "productName", "Kopi");
        set(OrderDetail.class, detail, "customizations", Collections.singletonList(group));

        Order order = new Order();
        set(Order.class, order, "orderDetails", Collections.singletonList(detail));

        assertTrue(order.hasCustomizations());
        assertEquals("Kopi (Sugar: Less Sugar)", order.getItemsSummary());
    }

    private static Customer customer(long id, String name, String phone) {
        Customer c = new Customer();
        set(Customer.class, c, "id", id);
        set(Customer.class, c, "name", name);
        set(Customer.class, c, "phone", phone);
        return c;
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
