package com.harmoni.pos.order.util;

/**
 * Phone handling shared by the customer search box and the register form. The backend
 * keys customers on their exact phone string, so the same input is normalised in one
 * place rather than several.
 */
public final class PhoneUtils {

    private PhoneUtils() {}

    /** Digits only, which is the form the search retries with when a formatted number fails. */
    public static String digits(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    /** Whether the input is long enough to be worth a round trip. */
    public static boolean isSearchable(String value) {
        return digits(value).length() >= 3;
    }

    /** Mirrors the customer service's rule: 5-30 characters from the phone alphabet. */
    public static boolean isValid(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        if (trimmed.length() < 5 || trimmed.length() > 30) return false;
        for (char c : trimmed.toCharArray()) {
            if (!(Character.isDigit(c) || c == '+' || c == '-' || c == '(' || c == ')' || c == ' ')) {
                return false;
            }
        }
        return true;
    }
}
