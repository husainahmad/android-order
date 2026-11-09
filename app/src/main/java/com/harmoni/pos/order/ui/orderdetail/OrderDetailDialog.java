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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.List;

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
    private OrderLineAdapter lineAdapter;

    private String pendingPrintText;
    private String pendingPrintLabel;
    private List<String> pendingKitchenTexts;
    private final ActivityResultLauncher<String> bluetoothPrintPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingKitchenTexts != null) {
                    doPrintKitchen(pendingKitchenTexts, 0);
                    pendingKitchenTexts = null;
                } else if (granted && pendingPrintText != null) {
                    doPrint(pendingPrintLabel, pendingPrintText);
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

        bindHeader();

        lineAdapter = new OrderLineAdapter();
        binding.itemsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.itemsRecycler.setHasFixedSize(true);
        binding.itemsRecycler.setAdapter(lineAdapter);

        binding.previewToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) renderPreview(checkedId);
        });
        renderPreview(R.id.previewItemsButton);

        MaterialButton payButton = binding.payButton;
        MaterialButton voidButton = binding.voidButton;
        payButton.setVisibility(order.isConfirmed() ? View.VISIBLE : View.GONE);
        voidButton.setVisibility(order.isConfirmed() || order.isPaid() ? View.VISIBLE : View.GONE);

        binding.printButton.setOnClickListener(v ->
                printText(getString(R.string.print), PrintUtils.buildReceiptText(order)));
        binding.kitchenButton.setOnClickListener(v -> printKitchen());
        payButton.setOnClickListener(v -> openPayment());
        voidButton.setOnClickListener(v -> confirmVoid());
        binding.closeButton.setOnClickListener(v -> dismiss());
    }

    private void bindHeader() {
        binding.orderNumberHeader.setText(order.getOrderNo());
        String meta = com.harmoni.pos.order.util.TimeUtils.toJakarta(order.getCreatedAt())
                + " • " + ("Takeaway".equalsIgnoreCase(order.getServiceTypeName())
                ? getString(R.string.takeaway) : getString(R.string.dine_in));
        binding.orderMetaHeader.setText(meta);
        binding.customerHeader.setText(order.getCustomerName().trim().isEmpty()
                ? getString(R.string.walk_in) : order.getCustomerName());
        String payment = order.getPaymentName();
        if ("QR".equalsIgnoreCase(payment)) payment = getString(R.string.payment_qris);
        binding.paymentHeader.setText(payment);
        binding.totalHeader.setText(com.harmoni.pos.order.util.CurrencyUtils.formatRp(order.getGrandTotal()));

        boolean paid = order.isPaid();
        boolean voided = order.isVoid();
        int bg = paid ? R.color.status_paid_bg
                : voided ? R.color.status_void_bg : R.color.status_confirmed_bg;
        int fg = paid ? R.color.status_paid
                : voided ? R.color.status_void : R.color.status_confirmed;
        binding.statusBadge.setText(order.getStatus());
        binding.statusBadge.setTextColor(requireContext().getColor(fg));
        binding.statusBadge.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(requireContext().getColor(bg)));
    }

    private void renderPreview(int checkedId) {
        boolean items = checkedId == R.id.previewItemsButton;
        binding.itemsRecycler.setVisibility(items ? View.VISIBLE : View.GONE);
        binding.textScrollContainer.setVisibility(items ? View.GONE : View.VISIBLE);
        if (items) {
            lineAdapter.submit(order);
        } else if (checkedId == R.id.previewReceiptButton) {
            binding.detailText.setText(PrintUtils.buildReceiptText(order));
        } else {
            binding.detailText.setText(PrintUtils.buildKitchenText(order));
        }
    }

    private void printKitchen() {
        List<String> texts = PrintUtils.buildKitchenTexts(order);
        if (texts.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.nothing_to_print), Toast.LENGTH_SHORT).show();
            return;
        }
        if (PrinterManager.TYPE_BLUETOOTH.equals(PrinterManager.getType())
                && !PrinterManager.isBluetoothPermissionGranted()) {
            pendingKitchenTexts = texts;
            bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            Toast.makeText(requireContext(), getString(R.string.bluetooth_permission_requesting), Toast.LENGTH_SHORT).show();
            return;
        }
        doPrintKitchen(texts, 0);
    }

    private void doPrintKitchen(List<String> texts, int index) {
        if (index >= texts.size()) {
            Toast.makeText(requireContext(), R.string.kitchen_print + " sent", Toast.LENGTH_SHORT).show();
            return;
        }
        PrinterManager.print(texts.get(index), new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                doPrintKitchen(texts, index + 1);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), getString(R.string.print_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null || getDialog().getWindow() == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        int width = Math.min((int) (screenWidth * 0.86f), (int) (1080 * density));
        int height = Math.min((int) (screenHeight * 0.90f), (int) (800 * density));
        getDialog().getWindow().setLayout(width, height);
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.bg_confirm_dialog);
    }

    private void printText(String label, String text) {
        if (PrinterManager.TYPE_BLUETOOTH.equals(PrinterManager.getType())
                && !PrinterManager.isBluetoothPermissionGranted()) {
            pendingPrintLabel = label;
            pendingPrintText = text;
            bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            Toast.makeText(requireContext(), getString(R.string.bluetooth_permission_requesting), Toast.LENGTH_SHORT).show();
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
                .setMessage(getString(R.string.void_this_order))
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
