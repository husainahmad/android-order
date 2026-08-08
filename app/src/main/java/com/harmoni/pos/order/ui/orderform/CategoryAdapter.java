package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    class CategoryHolder extends RecyclerView.ViewHolder {
        TextView nameText;

        CategoryHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.categoryName);
        }

        void bind(Category category, boolean selected) {
            nameText.setText(category.getName());

            if (selected) {
                itemView.setBackgroundResource(R.drawable.bg_category_selected);
            } else {
                itemView.setBackgroundResource(0);
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
