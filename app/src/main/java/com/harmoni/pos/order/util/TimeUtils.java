package com.harmoni.pos.order.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    public static final String TIMEZONE = "Asia/Jakarta";

    private TimeUtils() {}

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private static final SimpleDateFormat OUT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private static final SimpleDateFormat TODAY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    static {
        ISO.setTimeZone(TimeZone.getTimeZone("UTC"));
        OUT.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
    }

    public static String toJakarta(String isoUtc) {
        if (isoUtc == null || isoUtc.isEmpty()) return "";
        try {
            Date date;
            synchronized (ISO) {
                date = ISO.parse(isoUtc);
            }
            synchronized (OUT) {
                return OUT.format(date);
            }
        } catch (ParseException e) {
            return isoUtc;
        }
    }

    public static String todayDate() {
        synchronized (TODAY) {
            return TODAY.format(new Date());
        }
    }
}
