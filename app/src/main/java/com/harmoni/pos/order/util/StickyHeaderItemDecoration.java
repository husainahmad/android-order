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

    public StickyHeaderItemDecoration(StickyHeaderCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        int itemCount = state.getItemCount();
        if (itemCount == 0) return;

        if (parent.getAdapter() == null) return;

        View firstChild = parent.getChildAt(0);
        if (firstChild == null) return;

        int firstPosition = parent.getChildAdapterPosition(firstChild);
        if (firstPosition == RecyclerView.NO_POSITION) return;

        long currentHeaderId = callback.getHeaderId(firstPosition);
        if (currentHeaderId == -1) return;

        View header = callback.getHeaderView(firstPosition);
        if (header == null) return;

        // Always re-measure for correct width (rotation, window resize)
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        header.measure(widthSpec, heightSpec);
        header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
        int headerHeight = header.getMeasuredHeight();

        // Push up when next header collides
        int nextPosition = firstPosition + 1;
        if (nextPosition < itemCount) {
            long nextHeaderId = callback.getHeaderId(nextPosition);
            if (currentHeaderId != nextHeaderId) {
                View nextChild = parent.getChildAt(1);
                if (nextChild != null) {
                    int childTop = nextChild.getTop();
                    if (childTop < headerHeight) {
                        c.save();
                        c.translate(0, childTop - headerHeight);
                        header.draw(c);
                        c.restore();
                        return;
                    }
                }
            }
        }

        c.save();
        header.draw(c);
        c.restore();
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        long currHeaderId = callback.getHeaderId(position);
        if (currHeaderId == -1) return;

        // Add top offset only when header changes (first item of a group)
        boolean isFirstOfGroup = position == 0
                || callback.getHeaderId(position - 1) != currHeaderId;
        if (isFirstOfGroup) {
            View header = callback.getHeaderView(position);
            if (header != null) {
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                header.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY), heightSpec);
                outRect.top = header.getMeasuredHeight();
            }
        }
    }
}
