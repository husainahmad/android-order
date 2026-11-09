package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Category;

import java.util.ArrayList;
import java.util.List;

/** Horizontal category chip row: active chip is terracotta, inactive is outlined. */
public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.ChipHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private int selectedPosition = -1;

    public CategoryChipAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        if (selectedPosition >= categories.size()) {
            selectedPosition = -1;
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= categories.size()) {
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous >= 0) notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ChipHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new ChipHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipHolder holder, int position) {
        holder.bind(categories.get(position), position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ChipHolder extends RecyclerView.ViewHolder {
        final MaterialButton chip;

        ChipHolder(@NonNull View itemView) {
            super(itemView);
            chip = (MaterialButton) itemView;
        }

        void bind(Category category, boolean selected) {
            chip.setText(category.getName());
            chip.setIconResource(category.getIconRes() != 0
                    ? category.getIconRes() : CategoryIcons.forName(category.getName()));
            chip.setSelected(selected);
            chip.setContentDescription(selected
                    ? category.getName() + ", selected" : category.getName());
            chip.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                int previous = selectedPosition;
                selectedPosition = position;
                if (previous >= 0) notifyItemChanged(previous);
                notifyItemChanged(position);
                if (listener != null) listener.onCategoryClick(category);
            });
        }
    }
}
