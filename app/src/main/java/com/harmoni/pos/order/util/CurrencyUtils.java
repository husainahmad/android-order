package com.harmoni.pos.order.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {

    private static final DecimalFormatSymbols IDR_SYMBOLS;

    static {
        IDR_SYMBOLS = new DecimalFormatSymbols(Locale.US);
        IDR_SYMBOLS.setGroupingSeparator('.');
        IDR_SYMBOLS.setDecimalSeparator(',');
    }

    private static final DecimalFormat IDR = new DecimalFormat("#,##0", IDR_SYMBOLS);
    private static final DecimalFormat IDR2 = new DecimalFormat("#,##0.00", IDR_SYMBOLS);

    private CurrencyUtils() {}

    /** The single cashier money format: {@code Rp18.000}, no dot after Rp. */
    public static String formatRp(double value) {
        synchronized (IDR) {
            return "Rp" + IDR.format(value);
        }
    }

    /** Orders table format requested by the operations team: Rp. 28,000. */
    public static String formatRpOrders(double value) {
        synchronized (IDR) {
            return "Rp. " + IDR.format(value);
        }
    }

    public static String formatRp2(double value) {
        synchronized (IDR2) {
            return "Rp" + IDR2.format(value);
        }
    }

    public static String formatPlain(double value) {
        return IDR.format(value);
    }
}
