package com.harmoni.pos.order.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ReceiptLanguageTest {

    @Test
    public void knownLanguageReturnsItsMessages() {
        assertEquals("Terima kasih", ReceiptLanguage.messagesFor("id")[0]);
        assertEquals("Thank you", ReceiptLanguage.messagesFor("en")[0]);
    }

    @Test
    public void unknownLanguageFallsBackToIndonesian() {
        assertEquals("Terima kasih", ReceiptLanguage.messagesFor("xx")[0]);
    }

    @Test
    public void footerWithoutContextStillReturnsTwoLines() {
        String[] footer = ReceiptLanguage.footer(null);
        assertNotNull(footer);
        assertEquals(2, footer.length);
    }
}
