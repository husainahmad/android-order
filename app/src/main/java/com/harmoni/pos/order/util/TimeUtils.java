package com.harmoni.pos.order.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    public static final String TIMEZONE = "Asia/Jakarta";

    private TimeUtils() {}

    public static String toJakarta(String isoUtc) {
        if (isoUtc == null || isoUtc.isEmpty()) return "";
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            iso.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = iso.parse(isoUtc);
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            out.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
            return out.format(date);
        } catch (ParseException e) {
            return isoUtc;
        }
    }

    public static String todayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
