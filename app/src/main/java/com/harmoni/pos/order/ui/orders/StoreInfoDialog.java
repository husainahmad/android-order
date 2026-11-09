package com.harmoni.pos.order.ui.orders;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.remote.TokenManager;

/** Small, presentation-only store summary used by the sidebar and top selector. */
public final class StoreInfoDialog {

    private StoreInfoDialog() {}

    public static void show(Context context) {
        if (context == null) return;
        String store = fallback(TokenManager.getStoreName(), context.getString(R.string.sidebar_brand));
        String chain = TokenManager.getChainName();
        String brand = TokenManager.getBrandName();
        StringBuilder message = new StringBuilder(store);
        if (chain != null && !chain.trim().isEmpty()) message.append('\n').append(chain);
        if (brand != null && !brand.trim().isEmpty()) message.append('\n').append(brand);
        message.append("\n\n").append(context.getString(R.string.store_switching_unavailable));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.store_info_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
