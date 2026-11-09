package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Category;

import java.util.ArrayList;
import java.util.List;

/** Vertical category rail used by the order form. */
public class CategoryVerticalAdapter extends RecyclerView.Adapter<CategoryVerticalAdapter.Holder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private int selectedPosition = -1;

    public CategoryVerticalAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) categories.addAll(newCategories);
        if (selectedPosition >= categories.size()) selectedPosition = -1;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= categories.size()) return;
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous >= 0) notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_vertical, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(categories.get(position), position == selectedPosition);
    }

    @Override
    public int getItemCount() { return categories.size(); }

    class Holder extends RecyclerView.ViewHolder {
        final View row;
        final ImageView icon;
        final TextView name;

        Holder(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.categoryRow);
            icon = itemView.findViewById(R.id.categoryIcon);
            name = itemView.findViewById(R.id.categoryName);
        }

        void bind(Category category, boolean selected) {
            name.setText(category.getName());
            icon.setImageResource(category.getIconRes() != 0
                    ? category.getIconRes() : CategoryIcons.forName(category.getName()));
            row.setSelected(selected);
            itemView.setContentDescription(selected
                    ? category.getName() + ", selected" : category.getName());
            itemView.setOnClickListener(v -> {
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
