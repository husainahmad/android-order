package com.harmoni.pos.order.util;

import android.util.Base64;

public class JwtUtils {

    private JwtUtils() {}

    public static String decodePayload(String jwt) {
        if (jwt == null || jwt.isEmpty()) return "";
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return "";
        try {
            byte[] decoded = Base64.decode(padBase64Url(parts[1]), Base64.URL_SAFE);
            return new String(decoded, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static long getExp(String jwt) {
        String payload = decodePayload(jwt);
        if (payload.isEmpty()) return 0;
        try {
            int idx = payload.indexOf("\"exp\"");
            if (idx < 0) return 0;
            int colon = payload.indexOf(':', idx);
            int end = payload.indexOf(',', colon);
            if (end < 0) end = payload.indexOf('}', colon);
            if (colon < 0 || end < 0) return 0;
            String val = payload.substring(colon + 1, end).trim();
            return (long) Double.parseDouble(val);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String padBase64Url(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() % 4 != 0) sb.append('=');
        return sb.toString();
    }
}
