package com.harmoni.pos.order.ui.orderform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.databinding.DialogProductDetailBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailDialog extends DialogFragment {

    public interface OnSkuListener {
        void onAdd(Product product, Sku sku);
        void onRemove(Product product, Sku sku);
    }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DialogProductDetailBinding binding;
    private Product product;
    private OnSkuListener listener;

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setOnSkuListener(OnSkuListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.productNameText.setText(product.getName());
        bindImage(product);
        setupSkus(product);
        binding.closeButton.setOnClickListener(v -> dismiss());
    }

    private void setupSkus(Product product) {
        binding.skuContainer.removeAllViews();
        if (product.getSkus() == null || product.getSkus().isEmpty()) {
            MaterialButton empty = new MaterialButton(requireContext());
            empty.setEnabled(false);
            empty.setText(R.string.cart_empty);
            empty.setTextSize(14);
            binding.skuContainer.addView(empty);
            return;
        }
        for (Sku sku : product.getSkus()) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_sku_row, binding.skuContainer, false);
            ((TextView) row.findViewById(R.id.skuNameText)).setText(sku.getName());
            row.findViewById(R.id.skuAddButton).setOnClickListener(v -> {
                if (listener != null) listener.onAdd(product, sku);
            });
            row.findViewById(R.id.skuRemoveButton).setOnClickListener(v -> {
                if (listener != null) listener.onRemove(product, sku);
            });
            binding.skuContainer.addView(row);
        }
    }

    private void bindImage(Product product) {
        if (product.getProductImage() == null
                || product.getProductImage().getImageBlob().isEmpty()) {
            binding.productImage.setImageResource(R.color.bg_light);
            binding.imageProgress.setVisibility(View.GONE);
            return;
        }
        binding.imageProgress.setVisibility(View.VISIBLE);
        String blob = product.getProductImage().getImageBlob();
        Context context = requireContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeBase64(blob);
            MAIN.post(() -> {
                if (bitmap != null) {
                    binding.productImage.setImageBitmap(bitmap);
                }
                binding.imageProgress.setVisibility(View.GONE);
            });
        });
    }

    private static Bitmap decodeBase64(String blob) {
        try {
            byte[] data = Base64.decode(blob, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
