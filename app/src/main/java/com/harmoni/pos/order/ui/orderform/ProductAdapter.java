package com.harmoni.pos.order.ui.orderform;

import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProductAdapter extends ListAdapter<Product, ProductAdapter.ProductHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final OnProductClickListener listener;
    private final Map<Integer, Integer> cartQuantities = new HashMap<>();

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
                    && Objects.equals(imageUrl(o1), imageUrl(o2))
                    && o1.isOutOfStock() == o2.isOutOfStock()
                    && Objects.equals(o1.getBadge(), o2.getBadge());
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

    private static String imageUrl(Product product) {
        return product.getProductImage() != null ? product.getProductImage().getUrl() : null;
    }

    /** Updates the in-cart quantity badges without rebuilding the whole list. */
    public void setCartQuantities(List<CartItem> cart) {
        Map<Integer, Integer> next = new HashMap<>();
        if (cart != null) {
            for (CartItem item : cart) {
                next.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }
        if (next.equals(cartQuantities)) {
            return;
        }
        Set<Integer> touched = new HashSet<>(cartQuantities.keySet());
        touched.addAll(next.keySet());
        cartQuantities.clear();
        cartQuantities.putAll(next);

        List<Product> products = getCurrentList();
        for (int index = 0; index < products.size(); index++) {
            if (touched.contains(products.get(index).getId())) {
                notifyItemChanged(index);
            }
        }
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
        holder.shimmerPlaceholder.stopShimmer();
        holder.shimmerPlaceholder.setAlpha(1f);
        holder.shimmerPlaceholder.setVisibility(View.VISIBLE);
        holder.imageView.setVisibility(View.GONE);
        holder.imageView.setAlpha(1f);
        holder.imageView.setContentDescription(null);
        holder.monogram.setText("");
        holder.monogram.setVisibility(View.GONE);
        holder.monogram.setAlpha(1f);
        holder.badgeView.setVisibility(View.GONE);
        holder.qtyBadge.setVisibility(View.GONE);
        holder.outOfStockOverlay.setVisibility(View.GONE);
        holder.outOfStockText.setVisibility(View.GONE);
        holder.variantInfo.setText("");
    }

    class ProductHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imageView;
        final ProgressBar imageProgress;
        final TextView nameText;
        final TextView priceText;
        final TextView variantInfo;
        final TextView qtyBadge;
        final TextView monogram;
        final com.google.android.material.button.MaterialButton addButton;
        final com.facebook.shimmer.ShimmerFrameLayout shimmerPlaceholder;
        final View shimmerHighlight;
        final TextView badgeView;
        final View outOfStockOverlay;
        final TextView outOfStockText;

        ProductHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.productCard);
            imageView = itemView.findViewById(R.id.productImage);
            imageProgress = itemView.findViewById(R.id.imageProgress);
            nameText = itemView.findViewById(R.id.productName);
            priceText = itemView.findViewById(R.id.productPrice);
            variantInfo = itemView.findViewById(R.id.productVariantInfo);
            qtyBadge = itemView.findViewById(R.id.productQtyBadge);
            monogram = itemView.findViewById(R.id.productMonogram);
            addButton = itemView.findViewById(R.id.productAddButton);
            shimmerPlaceholder = itemView.findViewById(R.id.shimmerPlaceholder);
            shimmerHighlight = itemView.findViewById(R.id.shimmerHighlight);
            badgeView = itemView.findViewById(R.id.productBadge);
            outOfStockOverlay = itemView.findViewById(R.id.outOfStockOverlay);
            outOfStockText = itemView.findViewById(R.id.outOfStockText);
        }

        void bind(Product product) {
            nameText.setText(product.getName());
            imageView.setContentDescription(product.getName());

            Sku sku = defaultSku(product);
            priceText.setText(sku != null ? CurrencyUtils.formatRp(sku.getPrice()) : "");

            variantInfo.setText(variantLabel(product));

            monogram.setText(monogramFor(product));
            int quantity = cartQuantities.getOrDefault(product.getId(), 0);
            qtyBadge.setVisibility(quantity > 0 ? View.VISIBLE : View.GONE);
            qtyBadge.setText(String.valueOf(quantity));

            float density = itemView.getResources().getDisplayMetrics().density;
            card.setStrokeWidth(Math.round(density * (quantity > 0 ? 1.5f : 0.5f)));
            card.setStrokeColor(itemView.getContext().getColor(
                    quantity > 0 ? R.color.accent : R.color.md_outline));

            bindBadge(product);

            boolean outOfStock = product.isOutOfStock();
            monogram.setAlpha(outOfStock ? 0.45f : 1f);
            card.setCardBackgroundColor(itemView.getContext().getColor(
                    outOfStock ? R.color.md_surface_variant : R.color.md_surface));
            nameText.setTextColor(itemView.getContext().getColor(
                    outOfStock ? R.color.text_secondary : R.color.text_primary));
            priceText.setTextColor(itemView.getContext().getColor(
                    outOfStock ? R.color.text_secondary : R.color.accent));
            outOfStockOverlay.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
            outOfStockText.setVisibility(outOfStock ? View.VISIBLE : View.GONE);
            card.setClickable(!outOfStock);
            card.setEnabled(!outOfStock);
            addButton.setEnabled(!outOfStock);
            addButton.setAlpha(outOfStock ? 0.4f : 1f);

            itemView.setOnClickListener(v -> {
                if (listener == null || outOfStock) {
                    return;
                }
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                listener.onProductClick(product);
            });
            addButton.setOnClickListener(v -> {
                if (listener == null || outOfStock) {
                    return;
                }
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                listener.onProductClick(product);
            });

            bindImage(product);
        }

        private void bindBadge(Product product) {
            String badge = product.getBadge();
            if (badge != null && !badge.isEmpty() && !product.isOutOfStock()) {
                badgeView.setText(badge);
                badgeView.setVisibility(View.VISIBLE);
                badgeView.setBackgroundTintList(ColorStateList.valueOf(getBadgeColor(badge)));
            } else {
                badgeView.setVisibility(View.GONE);
            }
        }

        private String variantLabel(Product product) {
            List<Sku> skus = product.getSkus();
            if (skus == null || skus.isEmpty()) {
                return "";
            }
            List<String> names = new ArrayList<>();
            for (Sku sku : skus) {
                if (sku.getName() != null && !sku.getName().isEmpty()) {
                    names.add(sku.getName());
                }
            }
            if (names.isEmpty()) {
                return "";
            }
            if (names.size() == 1 && names.get(0).equalsIgnoreCase("regular")) {
                return "";
            }
            return String.join(" / ", names);
        }

        private int getBadgeColor(String badge) {
            String lower = badge.toLowerCase();
            if (lower.contains("new")) return itemView.getContext().getColor(R.color.primary);
            if (lower.contains("popular") || lower.contains("hot")) {
                return itemView.getContext().getColor(R.color.accent);
            }
            if (lower.contains("sale") || lower.contains("promo")) {
                return itemView.getContext().getColor(R.color.status_void);
            }
            return itemView.getContext().getColor(R.color.primary);
        }

        private void bindImage(Product product) {
            int productId = product.getId();
            if (product.getProductImage() == null
                    || product.getProductImage().getUrl().isEmpty()) {
                showMonogram();
                return;
            }
            shimmerPlaceholder.setVisibility(View.VISIBLE);
            shimmerPlaceholder.setAlpha(1f);
            shimmerPlaceholder.startShimmer();
            imageView.setVisibility(View.INVISIBLE);
            imageProgress.setVisibility(View.GONE);
            imageView.setAlpha(0f);

            String url = product.getProductImage().getUrl();
            ImageLoader.load(url, bitmap -> {
                int pos = getBindingAdapterPosition();
                if (pos >= 0 && pos < getItemCount()
                        && getItem(pos).getId() == productId) {
                    shimmerPlaceholder.stopShimmer();
                    if (bitmap != null) {
                        monogram.setVisibility(View.GONE);
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.animate().alpha(product.isOutOfStock() ? 0.45f : 1f)
                                .setDuration(150).start();
                        shimmerPlaceholder.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                            shimmerPlaceholder.setVisibility(View.GONE);
                            shimmerPlaceholder.setAlpha(1f);
                        }).start();
                    } else {
                        showMonogram();
                    }
                }
            });
        }

        private void showMonogram() {
            shimmerPlaceholder.stopShimmer();
            shimmerPlaceholder.setVisibility(View.GONE);
            shimmerPlaceholder.setAlpha(1f);
            imageView.setVisibility(View.GONE);
            monogram.setVisibility(View.VISIBLE);
        }

        /** First letters of up to two words, e.g. "Caramel Macchiato" -> "CM". */
        private String monogramFor(Product product) {
            String name = product.getName().trim();
            if (name.isEmpty()) return "?";
            String[] parts = name.split("\\s+");
            StringBuilder letters = new StringBuilder();
            letters.append(Character.toUpperCase(parts[0].charAt(0)));
            if (parts.length > 1 && !parts[1].isEmpty()) {
                letters.append(Character.toUpperCase(parts[1].charAt(0)));
            }
            return letters.toString();
        }
    }
}
