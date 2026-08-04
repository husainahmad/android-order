package com.harmoni.pos.order.ui.payment;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.PaymentRequest;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.databinding.DialogPaymentBinding;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.PrintUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentDialog extends DialogFragment {

    public interface OnPaymentSuccessListener {
        void onPaymentSuccess(Order paidOrder);
    }

    public interface OnPaymentDismissedListener {
        void onPaymentDismissed(Order order);
    }

    private static final String ARG_ORDER = "arg_order";
    private static final int PAYMENT_CASH = 1;
    private static final int PAYMENT_QR = 2;
    private static final int PAYMENT_CARD = 3;

    private DialogPaymentBinding binding;
    private Order order;
    private OnPaymentSuccessListener listener;
    private OnPaymentDismissedListener dismissedListener;
    private boolean paid;
    private double cashReceived;
    private int selectedPayment = PAYMENT_QR;

    public static PaymentDialog newInstance(Order order) {
        PaymentDialog dialog = new PaymentDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnPaymentSuccessListener(OnPaymentSuccessListener listener) {
        this.listener = listener;
    }

    public void setOnPaymentDismissedListener(OnPaymentDismissedListener listener) {
        this.dismissedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            order = (Order) getArguments().getSerializable(ARG_ORDER);
        }

        binding.orderInfoText.setText(String.format("Order : %s\nTotal : %s",
                order.getOrderNo(), CurrencyUtils.formatRp(order.getGrandTotal())));
        binding.cashTotalText.setText(CurrencyUtils.formatRp(order.getGrandTotal()));
        updateCash();

        binding.qrCard.setOnClickListener(v -> selectPayment(PAYMENT_QR));
        binding.cardCard.setOnClickListener(v -> selectPayment(PAYMENT_CARD));
        binding.cashCard.setOnClickListener(v -> selectPayment(PAYMENT_CASH));
        selectPayment(PAYMENT_QR);

        setupCashGrid();
        binding.clearButton.setOnClickListener(v -> {
            cashReceived = 0;
            updateCash();
        });
        binding.exactButton.setOnClickListener(v -> {
            cashReceived = order.getGrandTotal();
            updateCash();
        });
        binding.payButton.setOnClickListener(v -> onPay());
        binding.bayarButton.setOnClickListener(v -> onPay());
        binding.closeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.88);
            int height = (int) (metrics.heightPixels * 0.92);
            getDialog().getWindow().setLayout(width, height);
        }
    }

    private void selectPayment(int payment) {
        selectedPayment = payment;
        boolean cash = payment == PAYMENT_CASH;
        updateCard(binding.qrCard, binding.qrImage, binding.qrLabel, payment == PAYMENT_QR);
        updateCard(binding.cardCard, binding.cardImage, binding.cardLabel, payment == PAYMENT_CARD);
        updateCard(binding.cashCard, binding.cashImage, binding.cashLabel, cash);
        binding.cashPanel.setVisibility(cash ? View.VISIBLE : View.GONE);
        binding.payButton.setVisibility(cash ? View.GONE : View.VISIBLE);
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
                    cashReceived += Integer.parseInt(String.valueOf(v.getTag()));
                } catch (NumberFormatException ignored) {
                }
                updateCash();
            });
        }
    }

    private void updateCash() {
        binding.cashReceivedText.setText(CurrencyUtils.formatRp(cashReceived));
        double change = Math.max(0, cashReceived - order.getGrandTotal());
        binding.cashChangeText.setText(CurrencyUtils.formatRp(change));
    }

    private void onPay() {
        if (selectedPayment == PAYMENT_CASH && cashReceived < order.getGrandTotal()) {
            Toast.makeText(requireContext(), "Insufficient cash entered!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.payButton.setEnabled(false);
        binding.bayarButton.setEnabled(false);
        ApiClient.orderService().payOrder(new PaymentRequest(order.getId(), selectedPayment))
                .enqueue(new Callback<ApiResponse<Order>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                        binding.payButton.setEnabled(true);
                        binding.bayarButton.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Order paidOrder = response.body().getData();
                            printReceipt(paidOrder);
                            paid = true;
                            if (listener != null) listener.onPaymentSuccess(paidOrder);
                            dismiss();
                        } else {
                            error("Payment failed: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Order>> call, Throwable t) {
                        binding.payButton.setEnabled(true);
                        binding.bayarButton.setEnabled(true);
                        error("Network error: " + t.getMessage());
                    }
                });
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
        if (!paid && dismissedListener != null) {
            dismissedListener.onPaymentDismissed(order);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
