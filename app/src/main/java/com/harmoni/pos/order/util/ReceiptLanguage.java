package com.harmoni.pos.order.util;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Receipt footer text.
 * <p>
 * The footer follows the app language, so the ticket matches the screen the cashier
 * was just looking at. Only Latin transliterations are used, so any ESC/POS printer
 * prints them without a raster pipeline.
 */
public final class ReceiptLanguage {

    private static final Map<String, String[]> MESSAGES = new LinkedHashMap<>();

    static {
        MESSAGES.put("id", new String[]{"Terima kasih", "Sampai jumpa"});
        MESSAGES.put("in", new String[]{"Terima kasih", "Sampai jumpa"});
        MESSAGES.put("en", new String[]{"Thank you", "See you"});
        MESSAGES.put("jv", new String[]{"Matur nuwun", "Mangga rawuh"});
        MESSAGES.put("su", new String[]{"Hatur nuhun", "Sampurasun"});
        MESSAGES.put("ban", new String[]{"Suksma", "Matur suksma"});
        MESSAGES.put("min", new String[]{"Tarima kasih", "Sampai jumpo"});
        MESSAGES.put("bug", new String[]{"Kurre sumanga", "Tabe"});
        MESSAGES.put("mad", new String[]{"Mator sakalangkong", "Sampiyan"});
        MESSAGES.put("ar", new String[]{"Shukran", "Ila liqaa"});
        MESSAGES.put("ja", new String[]{"Arigatou", "Mata ne"});
        MESSAGES.put("ko", new String[]{"Kamsahamnida", "Annyeong"});
        MESSAGES.put("zh", new String[]{"Xiexie", "Zaijian"});
        MESSAGES.put("fr", new String[]{"Merci", "A bientot"});
        MESSAGES.put("de", new String[]{"Danke", "Bis bald"});
        MESSAGES.put("es", new String[]{"Gracias", "Hasta luego"});
    }

    private ReceiptLanguage() {}

    /** Two footer lines: thank-you and see-you, in the app's language. */
    public static String[] footer(Context context) {
        String code = LanguageManager.resolveLocale(context).getLanguage();
        if ("id".equals(code)) code = "in";
        return messagesFor(code);
    }

    public static String[] messagesFor(String code) {
        String[] messages = MESSAGES.get(code);
        if (messages == null) messages = MESSAGES.get("in");
        return messages;
    }
}