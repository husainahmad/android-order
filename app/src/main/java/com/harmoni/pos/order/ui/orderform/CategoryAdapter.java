package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    private static String monogram(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    class CategoryHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView iconText;
        TextView nameText;

        CategoryHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.categoryCard);
            iconText = itemView.findViewById(R.id.categoryIcon);
            nameText = itemView.findViewById(R.id.categoryName);
        }

        void bind(Category category, boolean selected) {
            String name = category.getName();
            nameText.setText(name);
            iconText.setText(monogram(name));
            iconText.setSelected(selected);

            if (selected) {
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.accent));
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.accent));
                iconText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent));
                nameText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.md_surface_container_high));
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.md_outline_variant));
                iconText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_on_primary_container));
                nameText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }

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