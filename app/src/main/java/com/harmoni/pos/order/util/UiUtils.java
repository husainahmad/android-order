package com.harmoni.pos.order.util;

import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge helpers so espresso-gradient headers draw under the
 * transparent system bars without their content colliding with them.
 */
public final class UiUtils {

    private UiUtils() {
    }

    /** Pads the top of {@code header} by the status-bar inset (drawn edge-to-edge). */
    public static void applyStatusBarTopInset(@NonNull View header) {
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(),
                    top,
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }
}
