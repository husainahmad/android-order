package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private int selectedPosition = -1;

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < categories.size()) {
            int previous = selectedPosition;
            selectedPosition = position;
            if (previous >= 0) notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView iconView;
        TextView nameText;
        ImageView selectedIcon;

        CategoryHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.categoryCard);
            iconView = itemView.findViewById(R.id.categoryIcon);
            nameText = itemView.findViewById(R.id.categoryName);
            selectedIcon = itemView.findViewById(R.id.selectedIcon);
        }

        void bind(Category category, boolean selected) {
            String name = category.getName();
            nameText.setText(name);

            // Set icon if available, fallback to default
            int iconRes = category.getIconRes();
            if (iconRes != 0) {
                iconView.setImageResource(iconRes);
            } else {
                iconView.setImageResource(R.drawable.ic_category_default);
            }

            // Set checked state for state-list drawables (bg, stroke, text, icon bg)
            card.setChecked(selected);

            // Show/hide check icon
            selectedIcon.setVisibility(selected ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                int previous = selectedPosition;
                selectedPosition = getBindingAdapterPosition();
                if (previous >= 0) notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
                if (listener != null) listener.onCategoryClick(category);
            });
        }
    }
}