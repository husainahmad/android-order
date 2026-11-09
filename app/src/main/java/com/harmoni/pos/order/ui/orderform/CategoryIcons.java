package com.harmoni.pos.order.ui.orderform;

import com.harmoni.pos.order.R;

import java.util.Locale;

/** Maps a category name to a distinct icon. UI-only; no data changes. */
public final class CategoryIcons {

    private CategoryIcons() {}

    public static int forName(String name) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (value.contains("non")) return R.drawable.ic_cat_non_coffee;
        if (value.contains("coffee") || value.contains("kopi")) return R.drawable.ic_cat_coffee;
        if (value.contains("snack") || value.contains("cemilan")) return R.drawable.ic_cat_snack;
        if (value.contains("food") || value.contains("makan")) return R.drawable.ic_cat_food;
        if (value.contains("brew") || value.contains("seduh")) return R.drawable.ic_cat_brewed;
        if (value.contains("additional") || value.contains("tambahan")) return R.drawable.ic_cat_additional;
        return R.drawable.ic_category_default;
    }
}
