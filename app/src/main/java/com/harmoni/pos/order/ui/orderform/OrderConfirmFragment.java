package com.harmoni.pos.order.ui.orderform;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.databinding.FragmentOrderConfirmBinding;
import com.harmoni.pos.order.ui.payment.PaymentDialog;
import com.harmoni.pos.order.util.CurrencyUtils;

public class OrderConfirmFragment extends DialogFragment {

    private FragmentOrderConfirmBinding binding;
    private OrderFormViewModel viewModel;
    private ConfirmItemAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.ThemeOverlay_OrderConfirm);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderConfirmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OrderFormViewModel.class);

        adapter = new ConfirmItemAdapter();
        binding.detailRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.detailRecycler.setAdapter(adapter);

        binding.customerInput.addTextChangedListener(customerWatcher);
        binding.discountInput.addTextChangedListener(discountWatcher);
        binding.remarkInput.addTextChangedListener(remarkWatcher);
        binding.backButton.setOnClickListener(v -> dismiss());
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.proceedButton.setOnClickListener(v -> onProceed());

        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            adapter.submitList(cart);
            updateTotals();
            int count = 0;
            if (cart != null) {
                for (CartItem item : cart) {
                    count += item.getQuantity();
                }
            }
            binding.itemCountText.setText(getString(R.string.item_count, count));
        });
        viewModel.getSubmitting().observe(getViewLifecycleOwner(), submitting ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(submitting) ? View.VISIBLE : View.GONE));
        viewModel.getConfirmedOrder().observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                openPayment(order);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isEmpty()) {
                binding.errorText.setVisibility(View.GONE);
            } else {
                binding.errorText.setText(message);
                binding.errorText.setVisibility(View.VISIBLE);
            }
        });

        OrderFormViewModel.OrderTab tab = viewModel.getActiveTab();
        if (tab != null) {
            binding.customerInput.setText(tab.customer);
            binding.discountInput.setText(tab.discount);
            binding.remarkInput.setText(tab.remark);
            binding.orderNumberText.setText(getString(R.string.order_num, tab.id));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private final TextWatcher customerWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            viewModel.setCustomer(s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    private final TextWatcher discountWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            viewModel.setDiscount(s.toString());
            updateTotals();
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    private final TextWatcher remarkWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            viewModel.setRemark(s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    private void updateTotals() {
        double subtotal = viewModel.getSubtotal();
        double discount = viewModel.getActiveDiscount();
        double total = Math.max(0, subtotal - discount);
        binding.subtotalText.setText(CurrencyUtils.formatRp(subtotal));
        binding.discountText.setText(CurrencyUtils.formatRp2(discount));
        binding.totalText.setText(CurrencyUtils.formatRp(total));
    }

    private void onProceed() {
        String customer = binding.customerInput.getText().toString();
        String discount = binding.discountInput.getText().toString();
        String remark = binding.remarkInput.getText().toString();
        if (viewModel.getCart().getValue() == null || viewModel.getCart().getValue().isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.confirmOrder(customer, discount, remark);
    }

    private void openPayment(Order order) {
        PaymentDialog dialog = PaymentDialog.newInstance(order);
        dialog.setOnPaymentSuccessListener(paid -> {
            int nextId = viewModel.removeActiveTab();
            dismiss();
        });
        dialog.setOnPaymentDismissedListener(order1 -> viewModel.resetConfirmedOrder());
        dialog.show(getParentFragmentManager(), "payment");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
