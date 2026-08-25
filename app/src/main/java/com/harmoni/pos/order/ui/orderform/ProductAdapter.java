package com.harmoni.pos.order.ui.orderform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductAdapter extends ListAdapter<Product, ProductAdapter.ProductHolder> {

    private static final int DEFAULT_IMAGE_RES = R.drawable.ic_default_product;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final OnProductClickListener listener;

    public ProductAdapter(OnProductClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product o1, @NonNull Product o2) {
            return o1.getId() == o2.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product o1, @NonNull Product o2) {
            Sku s1 = defaultSku(o1);
            Sku s2 = defaultSku(o2);
            return Objects.equals(o1.getName(), o2.getName())
                    && Objects.equals(s1 != null ? s1.getPrice() : null, s2 != null ? s2.getPrice() : null)
                    && Objects.equals(imageBlob(o1), imageBlob(o2));
        }
    };

    private static Sku defaultSku(Product product) {
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

    private static String imageBlob(Product product) {
        return product.getProductImage() != null ? product.getProductImage().getImageBlob() : null;
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
        holder.bind(getItem(position));
    }

    public void submitList(List<Product> newProducts) {
        super.submitList(newProducts == null ? null : new ArrayList<>(newProducts));
    }

    @Override
    public void onViewRecycled(@NonNull ProductHolder holder) {
        super.onViewRecycled(holder);
        holder.imageProgress.setVisibility(View.GONE);
        holder.shimmerPlaceholder.setVisibility(View.VISIBLE);
        holder.imageView.setVisibility(View.GONE);
        holder.imageView.setImageResource(DEFAULT_IMAGE_RES);
        holder.imageView.setContentDescription(null);
        holder.badgeView.setVisibility(View.GONE);
        holder.outOfStockOverlay.setVisibility(View.GONE);
        holder.outOfStockText.setVisibility(View.GONE);
    }

    class ProductHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ProgressBar imageProgress;
        final TextView nameText;
        final TextView priceText;
        final com.google.android.material.button.MaterialButton addButton;
        final View shimmerPlaceholder;
        final TextView badgeView;
        final View outOfStockOverlay;
        final TextView outOfStockText;
        private Animation shimmerAnim;

        ProductHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.productImage);
            imageProgress = itemView.findViewById(R.id.imageProgress);
            nameText = itemView.findViewById(R.id.productName);
            priceText = itemView.findViewById(R.id.productPrice);
            addButton = itemView.findViewById(R.id.productAddButton);
            shimmerPlaceholder = itemView.findViewById(R.id.shimmerPlaceholder);
            badgeView = itemView.findViewById(R.id.productBadge);
            outOfStockOverlay = itemView.findViewById(R.id.outOfStockOverlay);
            outOfStockText = itemView.findViewById(R.id.outOfStockText);
            shimmerAnim = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.shimmer_animation);
        }

        void bind(Product product) {
            nameText.setText(product.getName());
            imageView.setContentDescription(product.getName());
            Sku sku = defaultSku(product);
            if (sku != null) {
                priceText.setText(CurrencyUtils.formatRp(sku.getPrice()));
            } else {
                priceText.setText("");
            }

            // Badge handling
            String badge = product.getBadge();
            if (badge != null && !badge.isEmpty()) {
                badgeView.setText(badge);
                badgeView.setVisibility(View.VISIBLE);
                // Change badge color based on type
                int badgeColor = getBadgeColor(badge);
                badgeView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(badgeColor));
            } else {
                badgeView.setVisibility(View.GONE);
            }

            // Out of stock handling
            boolean outOfStock = product.isOutOfStock();
            outOfStockOverlay.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
            outOfStockText.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
            addButton.setEnabled(!outOfStock);
            addButton.setAlpha(outOfStock ? 0.5f : 1f);

            itemView.setOnClickListener(v -> {
                if (listener != null && !outOfStock) listener.onProductClick(product);
            });
            addButton.setOnClickListener(v -> {
                if (!outOfStock) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    if (listener != null) listener.onProductClick(product);
                }
            });
            bindImage(product);
        }

        private int getBadgeColor(String badge) {
            String lower = badge.toLowerCase();
            if (lower.contains("new")) return itemView.getContext().getColor(R.color.primary);
            if (lower.contains("popular") || lower.contains("hot")) return itemView.getContext().getColor(R.color.accent);
            if (lower.contains("sale") || lower.contains("promo")) return itemView.getContext().getColor(R.color.status_void);
            return itemView.getContext().getColor(R.color.primary);
        }

        private void bindImage(Product product) {
            int productId = product.getId();
            if (product.getProductImage() == null
                    || product.getProductImage().getImageBlob().isEmpty()) {
                showDefaultImage();
                return;
            }
            // Show shimmer while loading
            shimmerPlaceholder.setVisibility(View.VISIBLE);
            shimmerPlaceholder.startAnimation(shimmerAnim);
            imageView.setVisibility(View.GONE);
            imageProgress.setVisibility(View.GONE);

            String blob = product.getProductImage().getImageBlob();
            ImageLoader.load(productId, blob, bitmap -> {
                int pos = getBindingAdapterPosition();
                if (pos >= 0 && pos < getItemCount()
                        && getItem(pos).getId() == productId) {
                    shimmerPlaceholder.clearAnimation();
                    shimmerPlaceholder.setVisibility(View.GONE);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                    } else {
                        showDefaultImage();
                    }
                }
            });
        }

        private void showDefaultImage() {
            shimmerPlaceholder.clearAnimation();
            shimmerPlaceholder.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(DEFAULT_IMAGE_RES);
        }
    }
}