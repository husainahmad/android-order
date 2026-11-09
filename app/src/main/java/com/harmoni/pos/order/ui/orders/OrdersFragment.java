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

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private enum OrderFilter { ALL, PAID, CONFIRMED, VOID }

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrderAdapter orderAdapter;
    private final List<Order> allOrders = new ArrayList<>();
    private OrderFilter selectedFilter = OrderFilter.ALL;

    private String pendingSettlementText;
    private final ActivityResultLauncher<String> bluetoothPrintPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingSettlementText != null) {
                    doPrintSettlement(pendingSettlementText);
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

        setupRecyclers();
        setupButtons();
        setupFilters();
        setupScrollListener();
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
        binding.refreshButton.setOnClickListener(v -> viewModel.refresh());
        binding.settingsButton.setOnClickListener(v ->
                new SettingsDialogFragment().show(getParentFragmentManager(), "settings"));
        binding.logoutButton.setOnClickListener(v -> logout());
    }

    private void setupFilters() {
        binding.orderFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.filterPaid)) selectedFilter = OrderFilter.PAID;
            else if (checkedIds.contains(R.id.filterConfirmed)) selectedFilter = OrderFilter.CONFIRMED;
            else if (checkedIds.contains(R.id.filterVoid)) selectedFilter = OrderFilter.VOID;
            else selectedFilter = OrderFilter.ALL;
            applyFilters();
        });
    }

    private void setupScrollListener() {
        binding.ordersRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= orderAdapter.getItemCount() - 5) {
                    viewModel.loadMore();
                }
            }
        });
    }

    private void applyFilters() {
        List<Order> filtered = new ArrayList<>();
        for (Order order : allOrders) {
            if (matchesFilter(order)) filtered.add(order);
        }
        orderAdapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        binding.ordersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.ordersEmptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean filtering = selectedFilter != OrderFilter.ALL;
            binding.ordersEmptyView.emptyTitle.setText(filtering
                    ? R.string.no_orders_match : R.string.no_more_orders);
            binding.ordersEmptyView.emptySubtitle.setText(filtering
                    ? "" : getString(R.string.no_more_orders_subtitle));
        }
    }

    private boolean matchesFilter(Order order) {
        switch (selectedFilter) {
            case PAID: return order.isPaid();
            case CONFIRMED: return order.isConfirmed();
            case VOID: return order.isVoid();
            default: return true;
        }
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });
        viewModel.getLoadingMore().observe(getViewLifecycleOwner(), loadingMore -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(loadingMore) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isEmpty()) {
                binding.errorText.setVisibility(View.GONE);
                return;
            }
            if (message.contains("401") || message.contains("403") || message.contains("Session expired") || message.contains("Access denied")) {
                binding.errorText.setVisibility(View.GONE);
                return;
            }
            binding.errorText.setText(message);
            binding.errorText.setVisibility(View.VISIBLE);
        });

        viewModel.getOrders().observe(getViewLifecycleOwner(), this::onOrdersLoaded);
    }

    private void onOrdersLoaded(List<Order> orders) {
        allOrders.clear();
        if (orders != null) allOrders.addAll(orders);
        applyFilters();
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
                SettlementDialog dialog = SettlementDialog.newInstance(data);
                dialog.setOnPrintListener(OrdersFragment.this::printSettlement);
                dialog.show(getParentFragmentManager(), "settlement");
            }

            @Override
            public void onError(String message) {
                if (message != null && (message.contains("401") || message.contains("403") || message.contains("Session expired") || message.contains("Access denied"))) {
                    return;
                }
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
            Toast.makeText(requireContext(), getString(R.string.bluetooth_permission_requesting), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), getString(R.string.print_failed, message),
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
