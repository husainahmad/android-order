package com.harmoni.pos.order.ui.orderform;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.PaymentRequest;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.databinding.FragmentOrderConfirmBinding;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.PrintUtils;
import com.harmoni.pos.order.util.UiUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderConfirmFragment extends DialogFragment {

    public interface OnOrderCompletedListener {
        void onOrderCompleted(Order paidOrder);
    }

    private static final int PAYMENT_CASH = 1;
    private static final int PAYMENT_QR = 2;
    private static final int PAYMENT_CARD = 3;

    private static final String ARG_CUSTOMER = "arg_customer";
    private static final String ARG_DISCOUNT = "arg_discount";
    private static final String ARG_NOTE = "arg_note";
    private static final String ARG_ORDER_TYPE = "arg_order_type";

    private FragmentOrderConfirmBinding binding;
    private OrderFormViewModel viewModel;
    private ConfirmItemAdapter adapter;
    private OnOrderCompletedListener listener;
    private Order confirmedOrder;
    private int selectedPayment = PAYMENT_CASH;
    private double cashReceived;
    private boolean paid;
    private boolean working;

    private String passedCustomer = "";
    private String passedDiscount = "0";
    private String passedNote = "";
    private int passedOrderType = 1;

    public static OrderConfirmFragment newInstance(String customer, String discount, String note, int orderType) {
        OrderConfirmFragment fragment = new OrderConfirmFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER, customer);
        args.putString(ARG_DISCOUNT, discount);
        args.putString(ARG_NOTE, note);
        args.putInt(ARG_ORDER_TYPE, orderType);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnOrderCompletedListener(OnOrderCompletedListener listener) {
        this.listener = listener;
    }

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

        if (getArguments() != null) {
            passedCustomer = getArguments().getString(ARG_CUSTOMER, "");
            passedDiscount = getArguments().getString(ARG_DISCOUNT, "0");
            passedNote = getArguments().getString(ARG_NOTE, "");
            passedOrderType = getArguments().getInt(ARG_ORDER_TYPE, 1);
        }

        UiUtils.applyStatusBarTopInset(binding.confirmToolbar);

        adapter = new ConfirmItemAdapter();
        binding.detailRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.detailRecycler.setHasFixedSize(true);
        binding.detailRecycler.setItemAnimator(null);
        binding.detailRecycler.setAdapter(adapter);

        binding.backButton.setOnClickListener(v -> dismiss());
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.proceedButton.setOnClickListener(v -> onPay());

        binding.qrCard.setOnClickListener(v -> selectPayment(PAYMENT_QR));
        binding.cardCard.setOnClickListener(v -> selectPayment(PAYMENT_CARD));
        binding.cashCard.setOnClickListener(v -> selectPayment(PAYMENT_CASH));
        binding.clearButton.setOnClickListener(v -> {
            cashReceived = 0;
            updateCash();
        });
        binding.exactButton.setOnClickListener(v -> {
            cashReceived = confirmedOrder != null ? confirmedOrder.getGrandTotal() : getDisplayTotal();
            updateCash();
        });

        setupCashGrid();

        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            adapter.submitItems(cart);
            updateTotals();
            updateSummary();
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
                confirmedOrder = order;
                updatePaymentInfo();
                updateSummary();
                doPay(confirmedOrder);
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
            binding.orderNumberText.setText(getString(R.string.order_num, tab.id));
        }

        updateSummary();
        selectPayment(PAYMENT_CASH);
        updatePaymentInfo();
    }

    private void updateSummary() {
        OrderFormViewModel.OrderTab tab = viewModel.getActiveTab();
        String customer = passedCustomer;
        String discount = passedDiscount;
        String note = passedNote;
        if (tab != null) {
            if (customer.isEmpty()) customer = tab.customer;
            if (discount.isEmpty() || "0".equals(discount)) discount = tab.discount;
            if (note.isEmpty()) note = tab.remark;
        }

        binding.customerSummaryText.setText(customer.isEmpty() ? "-" : customer);
        binding.orderTypeSummaryText.setText(passedOrderType == 3 ? "Takeaway" : "Dine In");
        binding.discountSummaryText.setText("Rp" + CurrencyUtils.formatRp2(parseDiscount(discount)));
        binding.noteSummaryText.setText(note.isEmpty() ? "-" : note);
    }

    private double parseDiscount(String discountStr) {
        try {
            return Double.parseDouble(discountStr.isEmpty() ? "0" : discountStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private double getDisplayTotal() {
        double subtotal = viewModel.getSubtotal();
        double discount = viewModel.getActiveDiscount();
        return Math.max(0, subtotal - discount);
    }

    private void updateTotals() {
        double subtotal = viewModel.getSubtotal();
        double discount = viewModel.getActiveDiscount();
        double total = Math.max(0, subtotal - discount);
        binding.subtotalText.setText(CurrencyUtils.formatRp(subtotal));
        binding.discountText.setText(CurrencyUtils.formatRp2(discount));
        binding.totalText.setText(CurrencyUtils.formatRp(total));
    }

    private void updatePaymentInfo() {
        double total = confirmedOrder != null ? confirmedOrder.getGrandTotal() : getDisplayTotal();
        String orderNo = confirmedOrder != null ? confirmedOrder.getOrderNo() : "";
        String info;
        if (confirmedOrder != null) {
            info = String.format("Order : %s\nTotal : %s",
                    orderNo, CurrencyUtils.formatRp(confirmedOrder.getGrandTotal()));
        } else {
            info = String.format("Total : %s", CurrencyUtils.formatRp(total));
        }
        binding.paymentOrderInfoText.setText(info);
        binding.cashTotalText.setText(CurrencyUtils.formatRp(total));
        if (confirmedOrder == null) {
            cashReceived = 0;
            updateCash();
        }
    }

    private void selectPayment(int payment) {
        selectedPayment = payment;
        boolean cash = payment == PAYMENT_CASH;
        updateCard(binding.qrCard, binding.qrImage, binding.qrLabel, payment == PAYMENT_QR);
        updateCard(binding.cardCard, binding.cardImage, binding.cardLabel, payment == PAYMENT_CARD);
        updateCard(binding.cashCard, binding.cashImage, binding.cashLabel, cash);
        binding.cashPanel.setVisibility(cash ? View.VISIBLE : View.GONE);
        if (cash) {
            cashReceived = 0;
            updateCash();
        }
    }

    private void updateCard(LinearLayout card, ImageView image, TextView label, boolean selected) {
        card.setBackgroundResource(selected ? R.drawable.bg_payment_option_selected
                : R.drawable.bg_payment_option);
        image.setColorFilter(ContextCompat.getColor(requireContext(),
                selected ? R.color.accent : R.color.gray_button));
        label.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.text_primary : R.color.text_secondary));
        label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setupCashGrid() {
        GridLayout grid = binding.cashGrid;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            child.setOnClickListener(v -> {
                try {
                    cashReceived += Double.parseDouble(String.valueOf(v.getTag()));
                } catch (NumberFormatException ignored) {
                }
                updateCash();
            });
        }
    }

    private void updateCash() {
        double total = confirmedOrder != null ? confirmedOrder.getGrandTotal() : getDisplayTotal();
        binding.cashReceivedText.setText(CurrencyUtils.formatRp(cashReceived));
        double change = Math.max(0, cashReceived - total);
        binding.cashChangeText.setText(CurrencyUtils.formatRp(change));
    }

    private void onPay() {
        if (confirmedOrder != null) {
            doPay(confirmedOrder);
            return;
        }

        String customer = passedCustomer;
        String discount = passedDiscount;
        String remark = passedNote;
        if (viewModel.getCart().getValue() == null || viewModel.getCart().getValue().isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        working = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.cancelButton.setEnabled(false);
        binding.proceedButton.setEnabled(false);
        binding.qrCard.setClickable(false);
        binding.cardCard.setClickable(false);
        binding.cashCard.setClickable(false);

        viewModel.confirmOrder(customer, discount, remark, passedOrderType);
    }

    private void doPay(Order order) {
        if (selectedPayment == PAYMENT_CASH && cashReceived < order.getGrandTotal()) {
            Toast.makeText(requireContext(), "Insufficient cash entered!", Toast.LENGTH_SHORT).show();
            return;
        }

        working = true;
        setButtonsEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        ApiClient.orderService().payOrder(new PaymentRequest(order.getId(), selectedPayment))
                .enqueue(new Callback<ApiResponse<Order>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                        working = false;
                        binding.progressBar.setVisibility(View.GONE);
                        setButtonsEnabled(true);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Order paidOrder = response.body().getData();
                            printReceipt(paidOrder);
                            paid = true;
                            if (listener != null) listener.onOrderCompleted(paidOrder);
                            dismiss();
                        } else {
                            error("Payment failed: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                        working = false;
                        binding.progressBar.setVisibility(View.GONE);
                        setButtonsEnabled(true);
                        error("Network error: " + t.getMessage());
                    }
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.proceedButton.setEnabled(enabled);
        binding.cancelButton.setEnabled(enabled);
        binding.qrCard.setClickable(enabled);
        binding.cardCard.setClickable(enabled);
        binding.cashCard.setClickable(enabled);
        binding.clearButton.setEnabled(enabled);
        binding.exactButton.setEnabled(enabled);
    }

    private void printReceipt(Order paidOrder) {
        PrinterManager.print(PrintUtils.buildReceiptText(paidOrder),
                new PrinterManager.PrintCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Receipt sent to printer",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Print failed: " + message,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void error(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pay)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!paid) {
            viewModel.resetConfirmedOrder();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
