package com.harmoni.pos.order.ui.orderdetail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.ApiResponse;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.databinding.DialogOrderDetailBinding;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.ui.payment.PaymentDialog;
import com.harmoni.pos.order.util.PrintUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailDialog extends DialogFragment {

    public interface OnOrderChangedListener {
        void onOrderChanged();
    }

    private static final String ARG_ORDER = "arg_order";

    private DialogOrderDetailBinding binding;
    private Order order;
    private OnOrderChangedListener listener;

    private String pendingPrintText;
    private String pendingPrintLabel;
    private final ActivityResultLauncher<String> bluetoothPrintPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingPrintText != null) {
                    doPrint(pendingPrintLabel, pendingPrintText);
                } else if (!granted) {
                    boolean permanentlyDenied = !shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT);
                    if (permanentlyDenied) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Bluetooth Permission Required")
                                .setMessage("Bluetooth permission was denied permanently.\n\nOn Android 12+ this appears as \"Nearby devices\".\nEnable in: App info → Permissions → Nearby devices → Allow")
                                .setPositiveButton("Open Settings", (d, w) -> {
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                                        startActivity(intent);
                                    } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        Toast.makeText(requireContext(), "Bluetooth permission denied – enable in App Settings > Permissions", Toast.LENGTH_LONG).show();
                    }
                }
            });

    public static OrderDetailDialog newInstance(Order order) {
        OrderDetailDialog dialog = new OrderDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnOrderChangedListener(OnOrderChangedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            order = (Order) getArguments().getSerializable(ARG_ORDER);
        }
        if (order == null) {
            dismiss();
            return;
        }

        TextView detailText = binding.detailText;
        detailText.setText(PrintUtils.buildReceiptText(order));

        MaterialButton payButton = binding.payButton;
        MaterialButton voidButton = binding.voidButton;
        payButton.setVisibility(order.isConfirmed() ? View.VISIBLE : View.GONE);
        voidButton.setVisibility(order.isConfirmed() || order.isPaid() ? View.VISIBLE : View.GONE);

        binding.printButton.setOnClickListener(v ->
                printText(getString(R.string.print), PrintUtils.buildReceiptText(order)));
        binding.kitchenButton.setOnClickListener(v ->
                printText(getString(R.string.kitchen_print), PrintUtils.buildKitchenText(order)));
        payButton.setOnClickListener(v -> openPayment());
        voidButton.setOnClickListener(v -> confirmVoid());
        binding.closeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.5);
            int height = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.8);
            getDialog().getWindow().setLayout(width, height);
        }
    }

    private void printText(String label, String text) {
        if (PrinterManager.TYPE_BLUETOOTH.equals(PrinterManager.getType())
                && !PrinterManager.isBluetoothPermissionGranted()) {
            pendingPrintLabel = label;
            pendingPrintText = text;
            bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            Toast.makeText(requireContext(), "Requesting Bluetooth permission…", Toast.LENGTH_SHORT).show();
            return;
        }
        doPrint(label, text);
    }

    private void doPrint(String label, String text) {
        Toast.makeText(requireContext(), label + "...", Toast.LENGTH_SHORT).show();
        PrinterManager.print(text, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), label + " sent", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), label + " failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openPayment() {
        PaymentDialog dialog = PaymentDialog.newInstance(order, "", "0", "");
        dialog.setOnPaymentSuccessListener(paid -> {
            if (listener != null) listener.onOrderChanged();
            dismiss();
        });
        dialog.show(getParentFragmentManager(), "payment");
    }

    private void confirmVoid() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.void_order)
                .setMessage("Void this order?")
                .setPositiveButton(R.string.void_order, (d, w) -> voidOrder())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void voidOrder() {
        binding.voidButton.setEnabled(false);
        ApiClient.orderService().voidOrder(order.getId()).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    if (listener != null) listener.onOrderChanged();
                    dismiss();
                } else {
                    binding.voidButton.setEnabled(true);
                    error("Void failed: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.voidButton.setEnabled(true);
                error("Network error: " + t.getMessage());
            }
        });
    }

    private void error(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.void_order)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
