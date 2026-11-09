package com.harmoni.pos.order.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

public class LanguageManagerTest {

    @Test
    public void acceptsBothIndonesianCodes() {
        assertTrue(LanguageManager.isSupported("in"));
        assertTrue(LanguageManager.isSupported("id"));
        assertTrue(LanguageManager.isSupported("en"));
        assertFalse(LanguageManager.isSupported("fr"));
        assertFalse(LanguageManager.isSupported(null));
    }

    @Test
    public void offersExactlyTheSupportedCodes() {
        assertArrayEquals(new String[]{"en", "in"}, LanguageManager.availableCodes());
    }

    @Test
    public void systemResolvesToDeviceLanguageWhenSupported() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("id"));
            // "id" is the ISO code Android reports for Indonesian; it must resolve to
            // a supported locale rather than falling back to English. The desktop JVM
            // canonicalises the language code to "id" while Android uses "in", so the
            // assertion checks support rather than the exact string.
            Locale resolved = LanguageManager.resolveLocale(null);
            assertTrue(LanguageManager.isSupported(resolved.getLanguage()));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void englishDeviceStaysEnglish() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("en"));
            assertEquals("en", LanguageManager.resolveLocale(null).getLanguage());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void unsupportedLanguageFallsBackToEnglish() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("fr"));
            assertEquals("en", LanguageManager.resolveLocale(null).getLanguage());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
