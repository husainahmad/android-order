package com.harmoni.pos.order.ui.orderform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Integer, Bitmap> IMAGE_CACHE = new HashMap<>();

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

    class ProductHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ProgressBar imageProgress;
        final TextView nameText;
        final TextView priceText;

        ProductHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.productImage);
            imageProgress = itemView.findViewById(R.id.imageProgress);
            nameText = itemView.findViewById(R.id.productName);
            priceText = itemView.findViewById(R.id.productPrice);
        }

        void bind(Product product) {
            nameText.setText(product.getName());
            Sku sku = findDefaultSku(product);
            if (sku != null) {
                priceText.setText(CurrencyUtils.formatRp(sku.getPrice()));
            } else {
                priceText.setText("");
            }
            itemView.setOnClickListener(v -> {
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
            if (IMAGE_CACHE.containsKey(productId)) {
                imageView.setImageBitmap(IMAGE_CACHE.get(productId));
                imageProgress.setVisibility(View.GONE);
                return;
            }
            if (product.getProductImage() == null
                    || product.getProductImage().getImageBlob().isEmpty()) {
                imageView.setImageResource(R.color.bg_light);
                imageProgress.setVisibility(View.GONE);
                return;
            }
            imageProgress.setVisibility(View.VISIBLE);
            imageView.setImageResource(android.R.color.transparent);
            String blob = product.getProductImage().getImageBlob();
            Context context = itemView.getContext();
            EXECUTOR.execute(() -> {
                Bitmap bitmap = decodeBase64(blob);
                MAIN.post(() -> {
                    if (bitmap != null) {
                        IMAGE_CACHE.put(productId, bitmap);
                        if (getBindingAdapterPosition() >= 0
                                && getBindingAdapterPosition() < products.size()
                                && products.get(getBindingAdapterPosition()).getId() == productId) {
                            imageView.setImageBitmap(bitmap);
                        }
                    }
                    imageProgress.setVisibility(View.GONE);
                });
            });
        }
    }

    private static Bitmap decodeBase64(String blob) {
        try {
            byte[] data = Base64.decode(blob, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }
}
