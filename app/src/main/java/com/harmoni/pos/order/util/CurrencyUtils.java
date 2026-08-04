package com.harmoni.pos.order.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    private static final NumberFormat IDR = new DecimalFormat("#,##0");

    private CurrencyUtils() {}

    public static String formatRp(double value) {
        return "Rp." + IDR.format(value);
    }

    public static String formatRp2(double value) {
        return "Rp." + new DecimalFormat("#,##0.00").format(value);
    }

    public static String formatPlain(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
