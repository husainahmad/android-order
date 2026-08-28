package com.harmoni.pos.order.ui.orderform;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.databinding.DialogOrderPreviewBinding;
import com.harmoni.pos.order.util.CurrencyUtils;

public class OrderPreviewDialogFragment extends DialogFragment {

    private static final String ARG_CUSTOMER = "arg_customer";
    private static final String ARG_DISCOUNT = "arg_discount";
    private static final String ARG_NOTE = "arg_note";
    private static final String ARG_ORDER_TYPE = "arg_order_type";

    private DialogOrderPreviewBinding binding;
    private OrderFormViewModel viewModel;
    private ConfirmItemAdapter adapter;

    private String passedCustomer = "";
    private String passedDiscount = "0";
    private String passedNote = "";
    private int passedOrderType = 1;

    public static OrderPreviewDialogFragment newInstance(String customer, String discount, String note, int orderType) {
        OrderPreviewDialogFragment fragment = new OrderPreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER, customer);
        args.putString(ARG_DISCOUNT, discount);
        args.putString(ARG_NOTE, note);
        args.putInt(ARG_ORDER_TYPE, orderType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Not full screen - use default dialog style with rounded background
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);
        if (getArguments() != null) {
            passedCustomer = getArguments().getString(ARG_CUSTOMER, "");
            passedDiscount = getArguments().getString(ARG_DISCOUNT, "0");
            passedNote = getArguments().getString(ARG_NOTE, "");
            passedOrderType = getArguments().getInt(ARG_ORDER_TYPE, 1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogOrderPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OrderFormViewModel.class);

        adapter = new ConfirmItemAdapter();
        binding.previewRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.previewRecycler.setHasFixedSize(true);
        binding.previewRecycler.setAdapter(adapter);

        binding.closeButton.setOnClickListener(v -> dismiss());
        binding.closePreviewButton.setOnClickListener(v -> dismiss());

        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            adapter.submitItems(cart);
            boolean empty = cart == null || cart.isEmpty();
            binding.previewRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.emptyPreviewText.setVisibility(empty ? View.VISIBLE : View.GONE);
            int count = 0;
            if (cart != null) for (CartItem item : cart) count += item.getQuantity();
            binding.itemCountText.setText(getString(com.harmoni.pos.order.R.string.item_count, count));
            updateTotals();
        });

        updateTotals();
    }

    private void updateTotals() {
        double subtotal = viewModel.getSubtotal();
        binding.subtotalPreviewText.setText(CurrencyUtils.formatRp(subtotal));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Not full screen: 85% width, wrap content height, dim background
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            // Cap max width for tablets
            int maxWidth = (int) (560 * getResources().getDisplayMetrics().density);
            if (width > maxWidth) width = maxWidth;
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
