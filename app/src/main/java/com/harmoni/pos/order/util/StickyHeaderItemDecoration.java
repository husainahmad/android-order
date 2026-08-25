package com.harmoni.pos.order.util;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StickyHeaderItemDecoration extends RecyclerView.ItemDecoration {

    public interface StickyHeaderCallback {
        long getHeaderId(int position);
        View getHeaderView(int position);
        int getHeaderLayoutId();
    }

    private final StickyHeaderCallback callback;
    private View headerView;

    public StickyHeaderItemDecoration(StickyHeaderCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        int itemCount = state.getItemCount();
        if (itemCount == 0) return;

        RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null) return;

        // Find the first visible item
        View firstChild = parent.getChildAt(0);
        if (firstChild == null) return;

        int firstPosition = parent.getChildAdapterPosition(firstChild);
        if (firstPosition == RecyclerView.NO_POSITION) return;

        long currentHeaderId = callback.getHeaderId(firstPosition);
        View header = callback.getHeaderView(firstPosition);
        if (header == null) return;

        // Measure header if needed
        if (headerView == null || headerView.getWidth() != parent.getWidth()) {
            headerView = header;
            int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            headerView.measure(widthSpec, heightSpec);
            headerView.layout(0, 0, headerView.getMeasuredWidth(), headerView.getMeasuredHeight());
        }

        // Check if next item has different header
        int nextPosition = firstPosition + 1;
        if (nextPosition < itemCount) {
            long nextHeaderId = callback.getHeaderId(nextPosition);
            if (currentHeaderId != nextHeaderId) {
                // Next item has different header - push current header up
                View nextChild = parent.getChildAt(1);
                if (nextChild != null) {
                    int headerBottom = headerView.getMeasuredHeight();
                    int childTop = nextChild.getTop();
                    if (childTop < headerBottom) {
                        c.translate(0, childTop - headerBottom);
                        headerView.draw(c);
                        c.translate(0, headerBottom - childTop);
                        return;
                    }
                }
            }
        }

        // Draw header at top
        headerView.draw(c);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        // Add top offset for first item or when header changes
        if (position == 0) {
            View header = callback.getHeaderView(position);
            if (header != null) {
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                header.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY), heightSpec);
                outRect.top = header.getMeasuredHeight();
            }
        } else {
            long prevHeaderId = callback.getHeaderId(position - 1);
            long currHeaderId = callback.getHeaderId(position);
            if (prevHeaderId != currHeaderId) {
                View header = callback.getHeaderView(position);
                if (header != null) {
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    header.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY), heightSpec);
                    outRect.top = header.getMeasuredHeight();
                }
            }
        }
    }
}