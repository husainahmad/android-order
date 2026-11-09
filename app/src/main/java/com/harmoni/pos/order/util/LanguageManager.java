package com.harmoni.pos.order.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * The app's UI language.
 * <p>
 * The device locale is not a reliable answer for a shop till: the tablet is often left
 * in English while the staff read Indonesian. This stores an explicit choice (or
 * "system") and pins the resources to it, so every screen, dialog and toast uses the
 * same language as the setting.
 * <p>
 * Applying the locale through a per-activity {@link android.content.ContextWrapper}
 * affects resource lookup only; it does not change the device or any other app.
 */
public final class LanguageManager {

    /** Follow the device language. */
    public static final String SYSTEM = "system";

    private static final String PREFS = "language_prefs";
    private static final String KEY = "app_language";

    private static final String[] CODES = {"en", "in"};

    private LanguageManager() {}

    public static String get(Context context) {
        if (context == null) return SYSTEM;
        SharedPreferences prefs =
                context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String code = prefs.getString(KEY, SYSTEM);
        if (code == null) return SYSTEM;
        if (SYSTEM.equals(code) || isSupported(code)) return code;
        return SYSTEM;
    }

    public static void set(Context context, String code) {
        if (context == null) return;
        if (code == null || (!SYSTEM.equals(code) && !isSupported(code))) code = SYSTEM;
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, code).apply();
    }

    public static boolean isSupported(String code) {
        if (code == null) return false;
        // Android's resource qualifier for Indonesian is "in"; ISO 639-1 says "id".
        // Both are accepted so a device reporting either still matches.
        if ("id".equals(code)) return true;
        for (String supported : CODES) {
            if (supported.equals(code)) return true;
        }
        return false;
    }

    /** Normalises "id" to the "in" resource qualifier Android uses. */
    private static String normalise(String code) {
        return "id".equals(code) ? "in" : code;
    }

    /** The language codes offered by the settings screen, in display order. */
    public static String[] availableCodes() {
        return CODES.clone();
    }

    /** The concrete locale to use, resolving "system" to the device locale. */
    public static Locale resolveLocale(Context context) {
        String code = context == null ? SYSTEM : get(context);
        if (SYSTEM.equals(code)) {
            Locale device = Locale.getDefault();
            return isSupported(device.getLanguage())
                    ? new Locale(normalise(device.getLanguage()))
                    : new Locale("en");
        }
        return new Locale(normalise(code));
    }

    /** A context whose resources are pinned to the chosen language. */
    public static Context wrap(Context base) {
        if (base == null) return null;
        Locale locale = resolveLocale(base);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        Locale.setDefault(locale);
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new android.os.LocaleList(locale));
        }
        return base.createConfigurationContext(config);
    }

    /** Pins resources for the given context in place. Used by dialogs created from a context. */
    public static void applyToResources(Resources resources, Context context) {
        if (resources == null) return;
        Locale locale = resolveLocale(context);
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new android.os.LocaleList(locale));
        }
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}
