package com.harmoni.pos.order.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LineRemarkTest {

    @Test
    public void differentNotesProduceDifferentLineKeys() {
        List<Integer> options = Collections.singletonList(11);
        String plain = CartItem.lineKey(10, 101, options);
        String noOnion = CartItem.lineKey(10, 101, options, "no onion");
        String extraOnion = CartItem.lineKey(10, 101, options, "extra onion");

        assertNotEquals(plain, noOnion);
        assertNotEquals(noOnion, extraOnion);
    }

    @Test
    public void whitespaceOnlyNoteDoesNotChangeTheKey() {
        assertEquals(CartItem.lineKey(10, 101, Collections.emptyList()),
                CartItem.lineKey(10, 101, Collections.emptyList(), "   "));
    }

    @Test
    public void cartItemKeepsTheTrimmedNote() {
        CartItem item = new CartItem(10, "Coffee", 3, 101, "Large",
                50, 1, Collections.emptyList(), "  no onion  ");

        assertEquals("no onion", item.getRemark());
        assertTrue(item.getLineKey().contains("|r:no onion"));
    }

    @Test
    public void orderDetailRequestOmitsBlankNotes() {
        OrderDetailRequest plain = new OrderDetailRequest(10, "Coffee", 3,
                Arrays.asList(new OrderDetailSkuRequest(101, "Large", 1,
                        Collections.emptyList())), "   ");
        OrderDetailRequest noted = new OrderDetailRequest(10, "Coffee", 3,
                Arrays.asList(new OrderDetailSkuRequest(101, "Large", 1,
                        Collections.emptyList())), "no onion");

        assertEquals(null, plain.getRemark());
        assertEquals("no onion", noted.getRemark());
    }
}
