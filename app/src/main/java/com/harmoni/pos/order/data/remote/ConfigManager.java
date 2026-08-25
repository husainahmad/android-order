package com.harmoni.pos.order.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREFS = "server_prefs";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";

    private static final String DEFAULT_HOST = "103.150.197.7";
    private static final int DEFAULT_PORT = 8080;

    private static SharedPreferences prefs;

    private ConfigManager() {}

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    public static String getHost() {
        return prefs != null ? prefs.getString(KEY_HOST, DEFAULT_HOST) : DEFAULT_HOST;
    }

    public static int getPort() {
        return prefs != null ? prefs.getInt(KEY_PORT, DEFAULT_PORT) : DEFAULT_PORT;
    }

    public static void save(String host, int port) {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_HOST, host.trim().isEmpty() ? DEFAULT_HOST : host.trim())
                .putInt(KEY_PORT, port <= 0 ? DEFAULT_PORT : port)
                .apply();
    }

    public static String getBaseAuth() {
        return "http://" + getHost() + ":" + getPort() + "/";
    }

    public static String getBaseMenu() {
        return "http://" + getHost() + ":" + getPort() + "/";
    }

    public static String getBaseOrder() {
        return "http://" + getHost() + ":" + getPort() + "/";
    }
}
