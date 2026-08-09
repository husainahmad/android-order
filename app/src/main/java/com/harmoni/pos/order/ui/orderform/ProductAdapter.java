package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final List<Product> products = new ArrayList<>();
    private final OnProductClickListener listener;

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> newProducts) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    @Override
    public void onViewRecycled(@NonNull ProductHolder holder) {
        super.onViewRecycled(holder);
        holder.imageProgress.setVisibility(View.GONE);
        holder.imageView.setImageResource(R.drawable.bg_product_placeholder);
        holder.imageView.setContentDescription(null);
    }

    class ProductHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ProgressBar imageProgress;
        final TextView nameText;
        final TextView priceText;
        final ImageButton addButton;

        ProductHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.productImage);
            imageProgress = itemView.findViewById(R.id.imageProgress);
            nameText = itemView.findViewById(R.id.productName);
            priceText = itemView.findViewById(R.id.productPrice);
            addButton = itemView.findViewById(R.id.productAddButton);
        }

        void bind(Product product) {
            nameText.setText(product.getName());
            imageView.setContentDescription(product.getName());
            Sku sku = findDefaultSku(product);
            if (sku != null) {
                priceText.setText(CurrencyUtils.formatRp(sku.getPrice()));
            } else {
                priceText.setText("");
            }
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
            addButton.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                if (listener != null) listener.onProductClick(product);
            });
            bindImage(product);
        }

        private Sku findDefaultSku(Product product) {
            List<Sku> skus = product.getSkus();
            if (skus == null || skus.isEmpty()) {
                return null;
            }
            for (Sku sku : skus) {
                if (sku.getName().toLowerCase().contains("regular")) {
                    return sku;
                }
            }
            return skus.get(0);
        }

        private void bindImage(Product product) {
            int productId = product.getId();
            if (product.getProductImage() == null
                    || product.getProductImage().getImageBlob().isEmpty()) {
                imageView.setImageResource(R.drawable.bg_product_placeholder);
                imageProgress.setVisibility(View.GONE);
                return;
            }
            imageProgress.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.bg_product_placeholder);
            String blob = product.getProductImage().getImageBlob();
            ImageLoader.load(productId, blob, bitmap -> {
                if (getBindingAdapterPosition() >= 0
                        && getBindingAdapterPosition() < products.size()
                        && products.get(getBindingAdapterPosition()).getId() == productId) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.ic_image_off);
                    }
                }
                imageProgress.setVisibility(View.GONE);
            });
        }
    }
}
