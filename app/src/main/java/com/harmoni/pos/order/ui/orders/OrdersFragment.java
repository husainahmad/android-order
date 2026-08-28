package com.harmoni.pos.order.ui.orders;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Order;
import com.harmoni.pos.order.data.model.Settlement;
import com.harmoni.pos.order.data.remote.TokenManager;
import com.harmoni.pos.order.databinding.FragmentOrdersBinding;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.ui.orderdetail.OrderDetailDialog;
import com.harmoni.pos.order.ui.reports.DailyReportDialogFragment;
import com.harmoni.pos.order.ui.reports.SalesReportDialogFragment;
import com.harmoni.pos.order.ui.settings.SettingsDialogFragment;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrderAdapter orderAdapter;

    private String pendingSettlementText;
    private final ActivityResultLauncher<String> bluetoothPrintPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingSettlementText != null) {
                    doPrintSettlement(pendingSettlementText);
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OrdersViewModel.class);

        com.harmoni.pos.order.util.UiUtils.applyStatusBarTopInset(binding.ordersToolbar);
        setupRecyclers();
        setupButtons();
        observe();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel.getOrders().getValue() == null) {
            viewModel.loadOrders();
        }
    }

    private void setupRecyclers() {
        RecyclerView ordersRecycler = binding.ordersRecycler;
        orderAdapter = new OrderAdapter(this::openOrderDetail);
        ordersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        ordersRecycler.setHasFixedSize(true);
        ordersRecycler.setAdapter(orderAdapter);
    }

    private void setupButtons() {
        binding.dailyReportButton.setOnClickListener(v ->
                new DailyReportDialogFragment().show(getParentFragmentManager(), "daily_report"));
        binding.salesReportButton.setOnClickListener(v ->
                new SalesReportDialogFragment().show(getParentFragmentManager(), "sales_report"));
        binding.settlementButton.setOnClickListener(v -> loadSettlement());
        binding.refreshButton.setOnClickListener(v -> viewModel.loadOrders());
        binding.settingsButton.setOnClickListener(v ->
                new SettingsDialogFragment().show(getParentFragmentManager(), "settings"));
        binding.logoutButton.setOnClickListener(v -> logout());
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            binding.errorText.setVisibility(message != null && !message.isEmpty() ? View.VISIBLE : View.GONE);
            if (message != null) binding.errorText.setText(message);
        });

        viewModel.getOrders().observe(getViewLifecycleOwner(), this::onOrdersLoaded);
    }

    private void onOrdersLoaded(List<Order> orders) {
        orderAdapter.submitList(orders);
        boolean empty = orders == null || orders.isEmpty();
        binding.ordersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.ordersEmptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.ordersEmptyView.emptyIcon.setImageResource(R.drawable.ic_empty_orders);
            binding.ordersEmptyView.emptyTitle.setText(R.string.no_open_orders);
            binding.ordersEmptyView.emptySubtitle.setText(R.string.no_open_orders_subtitle);
        }
    }

    private void openOrderDetail(Order order) {
        OrderDetailDialog dialog = OrderDetailDialog.newInstance(order);
        dialog.setOnOrderChangedListener(viewModel::loadOrders);
        dialog.show(getParentFragmentManager(), "order_detail");
    }

    private void loadSettlement() {
        viewModel.loadSettlement(new OrdersViewModel.LoadCallback<Settlement>() {
            @Override
            public void onSuccess(Settlement data) {
                StringBuilder sb = new StringBuilder();
                sb.append("Total Orders : ").append(data.getTotalOrders()).append("\n");
                sb.append("Total Sales : ").append(CurrencyUtils.formatRp2(data.getTotalSales())).append("\n");
                sb.append("Total Discounts : ").append(CurrencyUtils.formatRp2(data.getTotalDiscounts())).append("\n");
                sb.append("Total Tax : ").append(CurrencyUtils.formatRp2(data.getTotalTax())).append("\n");
                sb.append("Total Net Sales : ").append(CurrencyUtils.formatRp2(data.getTotalNetSales())).append("\n\n");
                if (data.getPaymentBreakdown() != null) {
                    for (Map.Entry<String, Double> entry : data.getPaymentBreakdown().entrySet()) {
                        sb.append(entry.getKey()).append(" : ")
                                .append(CurrencyUtils.formatRp2(entry.getValue())).append("\n");
                    }
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settlement)
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.print, (d, w) ->
                                printSettlement(data))
                        .setNegativeButton(R.string.close, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settlement)
                        .setMessage(message)
                        .setPositiveButton(R.string.close, null)
                        .show();
            }
        });
    }

    private void printSettlement(Settlement data) {
        String text = "== SETTLEMENT REPORT =="
                + "\n" + "Total Orders : " + data.getTotalOrders()
                + "\n" + "Total Sales : " + CurrencyUtils.formatRp2(data.getTotalSales())
                + "\n" + "Total Discounts : " + CurrencyUtils.formatRp2(data.getTotalDiscounts())
                + "\n" + "Total Tax : " + CurrencyUtils.formatRp2(data.getTotalTax())
                + "\n" + "Total Net Sales : " + CurrencyUtils.formatRp2(data.getTotalNetSales())
                + "\n\n" + "Payment Breakdown:";
        if (data.getPaymentBreakdown() != null) {
            for (Map.Entry<String, Double> entry : data.getPaymentBreakdown().entrySet()) {
                text += "\n" + entry.getKey() + " : " + CurrencyUtils.formatRp2(entry.getValue());
            }
        }
        if (PrinterManager.TYPE_BLUETOOTH.equals(PrinterManager.getType())
                && !PrinterManager.isBluetoothPermissionGranted()) {
            pendingSettlementText = text;
            bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            Toast.makeText(requireContext(), "Requesting Bluetooth permission…", Toast.LENGTH_SHORT).show();
            return;
        }
        doPrintSettlement(text);
    }

    private void doPrintSettlement(String text) {
        PrinterManager.print(text, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), R.string.print + " sent",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "Print failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logout() {
        TokenManager.clearTokens();
        Navigation.findNavController(requireView()).navigate(R.id.action_order_pager_to_login);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
