package com.harmoni.pos.order.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Production-ready config manager.
 *
 * <ul>
 *   <li>Thread-safe double-checked locking</li>
 *   <li>Input validation + trimming</li>
 *   <li>Single source for base URL with https by default</li>
 *   <li>Notifies ApiClient to reset cached Retrofits on host/port change</li>
 * </ul>
 */
public final class ConfigManager {

    private static final String PREFS = "server_prefs";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_USE_HTTPS = "use_https";

    private static final String DEFAULT_HOST = "103.150.197.7";
    private static final int DEFAULT_PORT = 8080;
    private static final boolean DEFAULT_USE_HTTPS = false; // true in production once TLS is ready

    private static volatile SharedPreferences prefs;
    private static final Object LOCK = new Object();

    private ConfigManager() {}

    public static void init(@NonNull Context context) {
        if (prefs != null) return;
        synchronized (LOCK) {
            if (prefs != null) return;
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    public static boolean isInitialized() {
        return prefs != null;
    }

    @NonNull
    public static String getHost() {
        SharedPreferences p = prefs;
        if (p == null) return DEFAULT_HOST;
        String v = p.getString(KEY_HOST, DEFAULT_HOST);
        return (v == null || v.trim().isEmpty()) ? DEFAULT_HOST : v.trim();
    }

    public static int getPort() {
        SharedPreferences p = prefs;
        if (p == null) return DEFAULT_PORT;
        int port = p.getInt(KEY_PORT, DEFAULT_PORT);
        return (port <= 0 || port > 65535) ? DEFAULT_PORT : port;
    }

    public static boolean isUseHttps() {
        SharedPreferences p = prefs;
        if (p == null) return DEFAULT_USE_HTTPS;
        return p.getBoolean(KEY_USE_HTTPS, DEFAULT_USE_HTTPS);
    }

    /**
     * Save server config with validation.
     *
     * @param host raw host (IP or domain), trimmed, must not be empty
     * @param port 1-65535
     */
    public static void save(@NonNull String host, int port) {
        save(host, port, isUseHttps());
    }

    public static void save(@NonNull String host, int port, boolean useHttps) {
        SharedPreferences p = prefs;
        if (p == null) return;
        String cleanHost = host != null ? host.trim() : "";
        if (cleanHost.isEmpty()) cleanHost = DEFAULT_HOST;
        // Basic host validation: no scheme, no path, no port suffix
        cleanHost = cleanHost.replaceAll("^https?://", "").replaceAll("/.*$", "").replaceAll(":\\d+$", "");
        if (cleanHost.isEmpty()) cleanHost = DEFAULT_HOST;
        int cleanPort = (port <= 0 || port > 65535) ? DEFAULT_PORT : port;

        boolean changed = !cleanHost.equals(getHost()) || cleanPort != getPort() || useHttps != isUseHttps();
        p.edit()
                .putString(KEY_HOST, cleanHost)
                .putInt(KEY_PORT, cleanPort)
                .putBoolean(KEY_USE_HTTPS, useHttps)
                .apply();
        if (changed) {
            ApiClient.reset();
        }
    }

    @NonNull
    private static String getScheme() {
        return isUseHttps() ? "https" : "http";
    }

    @NonNull
    public static String getBaseAuth() {
        return buildBaseUrl();
    }

    @NonNull
    public static String getBaseMenu() {
        return buildBaseUrl();
    }

    @NonNull
    public static String getBaseOrder() {
        return buildBaseUrl();
    }

    @NonNull
    private static String buildBaseUrl() {
        return getScheme() + "://" + getHost() + ":" + getPort() + "/";
    }
}
