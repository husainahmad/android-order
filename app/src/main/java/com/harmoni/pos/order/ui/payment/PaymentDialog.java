package com.harmoni.pos.order.ui.payment;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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
    private static final String ARG_CUSTOMER = "arg_customer";
    private static final String ARG_DISCOUNT = "arg_discount";
    private static final String ARG_NOTE = "arg_note";
    private static final int PAYMENT_CASH = 1;
    private static final int PAYMENT_QR = 2;
    private static final int PAYMENT_CARD = 3;

    private DialogPaymentBinding binding;
    private PaymentLineAdapter summaryAdapter;
    private Order order;
    private String customerName = "";
    private String discountStr = "0";
    private String note = "";
    private OnPaymentSuccessListener listener;
    private OnPaymentDismissedListener dismissedListener;
    private boolean paid;
    private double cashReceived;
    private int selectedPayment = PAYMENT_CASH;

    private Order pendingPrintOrder;
    private final ActivityResultLauncher<String> bluetoothPrintPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingPrintOrder != null) {
                    doPrintReceipt(pendingPrintOrder);
                } else if (!granted) {
                    boolean permanentlyDenied = !shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT);
                    if (permanentlyDenied) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.bluetooth_permission_title))
                                .setMessage(getString(R.string.bluetooth_permission_permanently_denied))
                                .setPositiveButton(getString(R.string.open_settings), (d, w) -> {
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                                        startActivity(intent);
                                    } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                                })
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.bluetooth_permission_denied), Toast.LENGTH_LONG).show();
                    }
                }
            });

    public static PaymentDialog newInstance(Order order, String customerName, String discount, String note) {
        PaymentDialog dialog = new PaymentDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        args.putString(ARG_CUSTOMER, customerName);
        args.putString(ARG_DISCOUNT, discount);
        args.putString(ARG_NOTE, note);
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
        return com.harmoni.pos.order.util.DialogFrame.wrap(inflater.getContext(), binding.getRoot());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            order = (Order) getArguments().getSerializable(ARG_ORDER);
            customerName = getArguments().getString(ARG_CUSTOMER, "");
            discountStr = getArguments().getString(ARG_DISCOUNT, "0");
            note = getArguments().getString(ARG_NOTE, "");
        }

        binding.paymentTitleText.setText(getString(R.string.payment_order_title, order.getOrderNo()));
        int itemCount = 0;
        if (order.getOrderDetails() != null) {
            for (com.harmoni.pos.order.data.model.OrderDetail detail : order.getOrderDetails()) {
                if (detail == null) continue;
                if (detail.getOrderDetailSkus() != null && !detail.getOrderDetailSkus().isEmpty()) {
                    for (com.harmoni.pos.order.data.model.OrderDetailSku sku : detail.getOrderDetailSkus()) {
                        itemCount += (int) sku.getQuantity();
                    }
                } else {
                    itemCount++;
                }
            }
        }
        binding.paymentItemsText.setText(getString(R.string.payment_type_count,
                "Takeaway".equalsIgnoreCase(order.getServiceTypeName())
                        ? getString(R.string.takeaway) : getString(R.string.dine_in),
                getString(R.string.item_count, itemCount)));
        binding.subtotalSummaryText.setText(CurrencyUtils.formatRp(order.getSubTotal()));
        double discount = Math.max(0, parseDiscount(discountStr));
        binding.discountSummaryText.setText(discount > 0
                ? CurrencyUtils.formatRp(discount) : "-");
        binding.totalToPayText.setText(CurrencyUtils.formatRp(order.getGrandTotal()));
        binding.cashReceivedText.setText(CurrencyUtils.formatRp(0));
        binding.cashChangeText.setText(CurrencyUtils.formatRp(0));

        summaryAdapter = new PaymentLineAdapter();
        binding.summaryRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.summaryRecycler.setHasFixedSize(true);
        binding.summaryRecycler.setAdapter(summaryAdapter);
        summaryAdapter.submit(order);

        updatePrinterStatus();

        binding.paymentMethodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.qrButton) selectPayment(PAYMENT_QR);
            else if (checkedId == R.id.cardButton) selectPayment(PAYMENT_CARD);
            else selectPayment(PAYMENT_CASH);
        });
        binding.paymentMethodGroup.check(R.id.cashButton);
        selectPayment(PAYMENT_CASH);

        setupCashGrid();
        setupQuickAmounts();
        binding.exactButton.setOnClickListener(v -> {
            cashReceived = order.getGrandTotal();
            updateCash();
        });
        binding.backspaceButton.setOnClickListener(v -> {
            long current = (long) cashReceived;
            cashReceived = current / 10;
            updateCash();
        });
        binding.payButton.setOnClickListener(v -> onPay());
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.closeButton.setOnClickListener(v -> dismiss());
    }

    private void updatePrinterStatus() {
        // TODO: replace with a real printer connection state when PrinterManager exposes it.
        String address = PrinterManager.getAddress();
        boolean ready = address != null && !address.trim().isEmpty();
        boolean bluetooth = PrinterManager.TYPE_BLUETOOTH.equals(PrinterManager.getType());
        if (bluetooth) {
            ready = PrinterManager.isBluetoothPermissionGranted() && ready;
        }
        binding.printerStatusText.setText(ready
                ? R.string.printer_ready_receipt : R.string.printer_not_ready_receipt);
        binding.printerStatusText.setTextColor(ContextCompat.getColor(requireContext(),
                ready ? R.color.status_paid : R.color.status_confirmed));
        binding.printerStatusContainer.setBackgroundResource(ready
                ? R.drawable.bg_printer_ready : R.drawable.bg_printer_warning);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.88);
            int height = (int) (metrics.heightPixels * 0.92);
            getDialog().getWindow().setLayout(width, height);
            com.harmoni.pos.order.util.DialogFrame.dimBehind(getDialog().getWindow(), 0.35f);
        }
    }

    private double parseDiscount(String discountStr) {
        try {
            return Double.parseDouble(discountStr.isEmpty() ? "0" : discountStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void selectPayment(int payment) {
        selectedPayment = payment;
        boolean cash = payment == PAYMENT_CASH;
        binding.cashPanel.setVisibility(cash ? View.VISIBLE : View.GONE);
        binding.printerStatusContainer.setVisibility(View.VISIBLE);
        if (cash) {
            cashReceived = 0;
            updateCash();
        }
        updatePayButtonLabel();
    }

    private void updatePayButtonLabel() {
        String amount = CurrencyUtils.formatRp(order.getGrandTotal());
        binding.payButton.setText(getString(R.string.pay_amount, amount));
    }

    private void setupQuickAmounts() {
        android.view.ViewGroup row = binding.quickAmountScroll.getChildAt(0) instanceof android.view.ViewGroup
                ? (android.view.ViewGroup) binding.quickAmountScroll.getChildAt(0) : null;
        if (row == null) return;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child.getId() == R.id.exactButton) continue;
            Object tag = child.getTag();
            if (tag == null) continue;
            child.setOnClickListener(v -> {
                cashReceived = parseLongTag(v.getTag());
                updateCash();
            });
        }
    }

    private void setupCashGrid() {
        GridLayout grid = binding.cashGrid;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            child.setOnClickListener(v -> {
                String key = String.valueOf(v.getTag());
                if (!key.matches("\\d+")) return;
                long current = (long) cashReceived;
                String next = (current <= 0 ? "" : String.valueOf(current)) + key;
                try {
                    cashReceived = Long.parseLong(next);
                } catch (NumberFormatException ignored) {
                    // Keep the previous amount rather than resetting the entry.
                }
                updateCash();
            });
        }
    }

    private long parseLongTag(Object tag) {
        try {
            return Long.parseLong(String.valueOf(tag));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateCash() {
        binding.cashReceivedText.setText(CurrencyUtils.formatRp(cashReceived));
        double change = Math.max(0, cashReceived - order.getGrandTotal());
        binding.cashChangeText.setText(CurrencyUtils.formatRp(change));
    }

    private void onPay() {
        if (selectedPayment == PAYMENT_CASH && cashReceived < order.getGrandTotal()) {
            Toast.makeText(requireContext(), getString(R.string.insufficient_cash), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.payButton.setEnabled(false);
        ApiClient.orderService().payOrder(new PaymentRequest(order.getId(), selectedPayment))
                .enqueue(new Callback<ApiResponse<Order>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Order>> call, Response<ApiResponse<Order>> response) {
                        binding.payButton.setEnabled(true);
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
                        error("Network error: " + t.getMessage());
                    }
                });
    }

    private void printReceipt(Order paidOrder) {
        if (PrinterManager.TYPE_BLUETOOTH.equals(PrinterManager.getType())
                && !PrinterManager.isBluetoothPermissionGranted()) {
            pendingPrintOrder = paidOrder;
            bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            Toast.makeText(requireContext(), getString(R.string.bluetooth_permission_requesting), Toast.LENGTH_SHORT).show();
            return;
        }
        doPrintReceipt(paidOrder);
    }

    private void doPrintReceipt(Order paidOrder) {
        PrinterManager.print(PrintUtils.buildReceiptText(paidOrder),
                new PrinterManager.PrintCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), getString(R.string.receipt_sent),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), getString(R.string.print_failed, message),
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
