package com.harmoni.pos.order.ui.orderform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Customer;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.SelectedCustomization;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.data.model.TierPrice;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderFormCustomizationTest {

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
    }

    @Test
    public void sameSkuWithDifferentCustomizationsAreSeparateLines() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        Product product = product(1, "Kopi", 1);
        Sku sku = sku(11, "Regular", 17000);

        List<SelectedCustomization> withShot = new ArrayList<>();
        withShot.add(new SelectedCustomization(1, "Extra", 10, "Shot", 5000, 1));

        vm.addToCart(product, sku, withShot);
        vm.addToCart(product, sku, withShot);
        vm.addToCart(product, sku, new ArrayList<>());

        List<CartItem> cart = vm.getCart().getValue();
        assertEquals(2, cart.size());

        CartItem customized = null;
        CartItem plain = null;
        for (CartItem item : cart) {
            if (item.getCustomizations().isEmpty()) plain = item;
            else customized = item;
        }
        assertNotNull(customized);
        assertNotNull(plain);
        assertEquals(2, customized.getQuantity());
        assertEquals(1, plain.getQuantity());
        assertEquals(22000, customized.getUnitPrice(), 0.001);
        assertEquals(22000 * 2 + 17000, vm.getSubtotal(), 0.001);
    }

    @Test
    public void selectedCustomerLinksOnlyWhileNameStillMatches() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        Customer customer = customer(5, "Budi", "0812");

        vm.selectCustomer(customer);
        assertEquals("Budi", vm.getActiveTab().customer);
        assertNotNull(vm.getLinkedCustomer("Budi"));
        assertNull(vm.getLinkedCustomer("Sari"));

        vm.setCustomer("Sari");
        assertNull(vm.getLinkedCustomer("Sari"));
    }

    @Test
    public void clearCustomerReturnsToWalkIn() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        vm.selectCustomer(customer(5, "Budi", "0812"));

        vm.clearCustomer();

        assertEquals("", vm.getActiveTab().customer);
        assertNull(vm.getLinkedCustomer(""));
    }

    @Test
    public void editingTheCartRotatesTheIdempotencyKey() {
        OrderFormViewModel vm = new OrderFormViewModel();
        vm.ensureActiveTab();
        String before = vm.getActiveTab().idempotencyKey;

        vm.addToCart(product(1, "Kopi", 1), sku(11, "Regular", 17000));

        assertFalse(before.equals(vm.getActiveTab().idempotencyKey));
    }

    @Test
    public void cartLineKeyIsStableForSameChoices() {
        List<SelectedCustomization> a = Collections.singletonList(
                new SelectedCustomization(1, "G", 10, "A", 0, 1));
        List<SelectedCustomization> b = Collections.singletonList(
                new SelectedCustomization(1, "G", 10, "A", 0, 1));
        assertEquals(a.get(0).getRepeatedOptionIds(), b.get(0).getRepeatedOptionIds());
    }

    // --- reflection helpers -------------------------------------------------

    private static Product product(int id, String name, int categoryId) {
        Product p = new Product();
        setInt(Product.class, p, "id", id);
        set(Product.class, p, "name", name);
        setInt(Product.class, p, "categoryId", categoryId);
        return p;
    }

    private static Sku sku(int id, String name, double price) {
        Sku s = new Sku();
        setInt(Sku.class, s, "id", id);
        set(Sku.class, s, "name", name);
        TierPrice tierPrice = new TierPrice();
        setDouble(TierPrice.class, tierPrice, "price", price);
        set(Sku.class, s, "tierPrice", tierPrice);
        return s;
    }

    private static Customer customer(long id, String name, String phone) {
        Customer c = new Customer();
        setLong(Customer.class, c, "id", id);
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

    private static void setInt(Class<?> clazz, Object target, String field, int value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.setInt(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setLong(Class<?> clazz, Object target, String field, long value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.setLong(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setDouble(Class<?> clazz, Object target, String field, double value) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.setDouble(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
