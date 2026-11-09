package com.harmoni.pos.order.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.harmoni.pos.order.R;

/**
 * Gives a dialog a clear edge without a heavy shadow: a flat surface with a
 * 1dp outline, surrounded by a dimmed window behind it.
 */
public final class DialogFrame {

    private DialogFrame() {}

    public static View wrap(Context context, View content) {
        if (context == null || content == null) return content;
        if (content.getParent() instanceof MaterialCardView) return content;

        float density = context.getResources().getDisplayMetrics().density;
        int outer = Math.max(1, Math.round(2 * density));
        int stroke = Math.max(1, Math.round(1 * density));

        FrameLayout frame = new FrameLayout(context);
        frame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setPadding(outer, outer, outer, outer);

        MaterialCardView card = new MaterialCardView(context);
        card.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.setRadius(20 * density);
        card.setCardElevation(0f);
        card.setStrokeWidth(stroke);
        card.setStrokeColor(ContextCompat.getColor(context, R.color.md_outline));
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_surface));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setClipToOutline(true);
        card.setContentPadding(stroke, stroke, stroke, stroke);

        if (content.getParent() instanceof ViewGroup) {
            ((ViewGroup) content.getParent()).removeView(content);
        }
        card.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.addView(card);
        return frame;
    }

    /** Dims the page behind the dialog so the surface reads as a separate layer. */
    public static void dimBehind(Window window, float amount) {
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(amount);
    }
}
