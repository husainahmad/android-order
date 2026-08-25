package com.harmoni.pos.order.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    private static final NumberFormat IDR = new DecimalFormat("#,##0");
    private static final DecimalFormat IDR2 = new DecimalFormat("#,##0.00");

    private CurrencyUtils() {}

    public static String formatRp(double value) {
        synchronized (IDR) {
            return "Rp." + IDR.format(value);
        }
    }

    public static String formatRp2(double value) {
        synchronized (IDR2) {
            return "Rp." + IDR2.format(value);
        }
    }

    public static String formatPlain(double value) {
        return IDR.format(value);
    }
}
