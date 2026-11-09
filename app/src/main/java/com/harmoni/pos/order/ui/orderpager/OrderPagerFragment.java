package com.harmoni.pos.order.ui.orderpager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.databinding.FragmentOrderPagerBinding;
import com.harmoni.pos.order.ui.orderform.OrderFormViewModel;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.ui.orders.OrdersViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderPagerFragment extends Fragment {

    private FragmentOrderPagerBinding binding;
    private OrderFormViewModel viewModel;
    private OrdersViewModel ordersViewModel;
    private OrderPagerAdapter pagerAdapter;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderPagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        com.harmoni.pos.order.util.UiUtils.applyStatusBarTopInset(binding.topBar);

        viewModel = new ViewModelProvider(requireActivity()).get(OrderFormViewModel.class);
        ordersViewModel = new ViewModelProvider(requireActivity()).get(OrdersViewModel.class);
        pagerAdapter = new OrderPagerAdapter(requireActivity());

        setupStatusIndicators();
        setupStoreSelector();
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setOffscreenPageLimit(10);
        binding.viewPager.setSaveEnabled(true);

        if (savedInstanceState != null) {
            int savedItem = savedInstanceState.getInt("viewPagerCurrentItem", 0);
            binding.viewPager.post(() -> binding.viewPager.setCurrentItem(savedItem, false));
        }

        ordersViewModel.loadOrders();

        viewModel.getTabs().observe(getViewLifecycleOwner(), tabs -> {
            if (tabs == null) return;
            List<Integer> ids = new ArrayList<>();
            for (OrderFormViewModel.OrderTab tab : tabs) {
                ids.add(tab.id);
            }
            pagerAdapter.setOrderIds(ids);
            binding.viewPager.post(() -> renderHeaderTabs(tabs));
            if (ids.isEmpty()) {
                binding.viewPager.post(() -> {
                    if (binding != null && binding.viewPager.getCurrentItem() != 0) {
                        binding.viewPager.setCurrentItem(0, true);
                    }
                });
            }
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int orderId = pagerAdapter.getOrderIdAtPosition(position);
                if (orderId != -1) {
                    viewModel.switchTab(orderId);
                }
                updateTabSelection(position);
            }
        });
    }

    private void setupStoreSelector() {
        String storeName = com.harmoni.pos.order.data.remote.TokenManager.getStoreName();
        binding.storeNameText.setText(storeName == null || storeName.trim().isEmpty()
                ? getString(R.string.sidebar_brand) : storeName);
        binding.storeSelector.setOnClickListener(v ->
                com.harmoni.pos.order.ui.orders.StoreInfoDialog.show(requireContext()));
    }

    private void setupStatusIndicators() {
        updatePrinterStatus();
        ConnectivityManager connectivityManager =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            updateConnectionStatus(false);
            return;
        }
        // TODO: replace with the shared connectivity state once the app exposes one.
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updateConnectionStatus(isOnline(connectivityManager));
            }

            @Override
            public void onLost(@NonNull Network network) {
                updateConnectionStatus(isOnline(connectivityManager));
            }
        };
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (RuntimeException ignored) {
            // Older/OEM devices without default callback support: show the current snapshot.
        }
        updateConnectionStatus(isOnline(connectivityManager));
    }

    private boolean isOnline(ConnectivityManager connectivityManager) {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void updateConnectionStatus(boolean online) {
        if (!isAdded()) return;
        Runnable apply = () -> {
            if (binding == null) return;
            int color = ContextCompat.getColor(requireContext(),
                    online ? R.color.status_paid : R.color.status_void);
            binding.connectionText.setText(online ? R.string.status_online : R.string.status_offline);
            binding.connectionText.setTextColor(color);
            binding.connectionIcon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
            binding.connectionIcon.setContentDescription(getString(
                    online ? R.string.status_online : R.string.status_offline));
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply.run();
        } else {
            requireActivity().runOnUiThread(apply);
        }
    }

    private void updatePrinterStatus() {
        if (binding == null) return;
        // TODO: swap "configured" for the real printer connection state when PrinterManager exposes it.
        String address = PrinterManager.getAddress();
        boolean ready = address != null && !address.trim().isEmpty();
        int color = ContextCompat.getColor(requireContext(),
                ready ? R.color.status_paid : R.color.status_void);
        binding.printerText.setText(ready ? R.string.printer_ready : R.string.printer_not_ready);
        binding.printerText.setTextColor(color);
        binding.printerIcon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        binding.printerIcon.setContentDescription(getString(
                ready ? R.string.printer_ready : R.string.printer_not_ready));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.viewPager.post(() -> {
                if (binding.viewPager.getCurrentItem() != 0) {
                    binding.viewPager.setCurrentItem(0, false);
                }
                List<OrderFormViewModel.OrderTab> tabs = viewModel.getTabs().getValue();
                if (tabs != null) {
                    renderHeaderTabs(tabs);
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null && binding.viewPager != null) {
            outState.putInt("viewPagerCurrentItem", binding.viewPager.getCurrentItem());
        }
    }

    private String lastHeaderSignature;

    private void renderHeaderTabs(List<OrderFormViewModel.OrderTab> tabs) {
        if (tabs == null || binding == null) {
            return;
        }
        StringBuilder sig = new StringBuilder();
        for (OrderFormViewModel.OrderTab tab : tabs) {
            sig.append(tab.id).append(':').append(tab.label).append(';');
        }
        String signature = sig.toString();
        if (signature.equals(lastHeaderSignature)) {
            binding.viewPager.post(() -> updateTabSelection(binding.viewPager.getCurrentItem()));
            return;
        }
        lastHeaderSignature = signature;
        binding.tabContainer.removeAllViews();
        int currentPosition = binding.viewPager.getCurrentItem();

        addTabHeaderView(getString(R.string.orders), 0, -1, currentPosition == 0, false,
                R.drawable.ic_empty_cart);
        for (int i = 0; i < tabs.size(); i++) {
            OrderFormViewModel.OrderTab tab = tabs.get(i);
            int targetPosition = i + 1;
            addTabHeaderView(tab.label, targetPosition, tab.id, currentPosition == targetPosition, true);
        }
        addTabHeaderView(getString(R.string.new_order), -1, -1, false, false);
        updateTabSelection(currentPosition);
    }

    private void applyTabStyle(View chip, boolean selected) {
        chip.setSelected(selected);
        chip.setElevation(selected ? 6f : 1.5f);
        chip.setBackgroundResource(selected ? R.drawable.bg_order_tab_selected
                : R.drawable.bg_order_tab);
        TextView label = chip.findViewById(R.id.tabLabel);
        // Selected: dark text on vibrant accent (works in both light/dark via accent/onPrimary)
        label.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.md_on_primary : R.color.text_primary));
        ImageButton close = chip.findViewById(R.id.closeButton);
        close.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        selected ? R.color.md_on_primary : R.color.text_secondary)));
        ImageView icon = chip.findViewById(R.id.tabIcon);
        if (icon != null) {
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(),
                            selected ? R.color.md_on_primary : R.color.text_secondary)));
        }
    }

    /** Creates a new order tab and selects it. Used by the top bar and the sidebar. */
    public void openNewOrderTab() {
        if (binding == null || viewModel == null) return;
        int newOrderId = viewModel.createNewTab();
        binding.viewPager.post(() -> {
            int pos = pagerAdapter.getPositionForOrderId(newOrderId);
            if (pos != -1) {
                binding.viewPager.setCurrentItem(pos, true);
            }
        });
    }

    private void addTabHeaderView(String text, int targetPosition, int orderId,
                                  boolean isSelected, boolean isCloseable) {
        addTabHeaderView(text, targetPosition, orderId, isSelected, isCloseable, 0);
    }

    private void addTabHeaderView(String text, int targetPosition, int orderId,
                                  boolean isSelected, boolean isCloseable, int iconRes) {
        LinearLayout chip = (LinearLayout) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_order_tab, binding.tabContainer, false);
        TextView label = chip.findViewById(R.id.tabLabel);
        ImageButton close = chip.findViewById(R.id.closeButton);
        ImageView icon = chip.findViewById(R.id.tabIcon);

        label.setText(text);
        if (iconRes != 0 && icon != null) {
            icon.setImageResource(iconRes);
            icon.setVisibility(View.VISIBLE);
        }
        close.setVisibility(isCloseable ? View.VISIBLE : View.GONE);

        applyTabStyle(chip, isSelected);

        if (isCloseable) {
            close.setOnClickListener(v -> confirmAndRemoveTab(orderId));
        }
        chip.setOnClickListener(v -> {
            if (targetPosition == -1) {
                openNewOrderTab();
            } else {
                binding.viewPager.setCurrentItem(targetPosition, true);
            }
        });
        binding.tabContainer.addView(chip);
    }

    private void confirmAndRemoveTab(int orderId) {
        OrderFormViewModel.OrderTab tab = viewModel.getTab(orderId);
        String label = tab != null ? tab.label : String.valueOf(orderId);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_close)
                .setMessage(getString(R.string.close_order_prompt, label))
                .setPositiveButton(R.string.close, (d, w) -> {
                    int nextId = viewModel.removeTab(orderId);
                    binding.viewPager.post(() -> {
                        int pos = pagerAdapter.getPositionForOrderId(nextId);
                        if (nextId == -1) {
                            binding.viewPager.setCurrentItem(0, true);
                        } else if (pos != -1) {
                            binding.viewPager.setCurrentItem(pos, true);
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateTabSelection(int currentPosition) {
        for (int i = 0; i < binding.tabContainer.getChildCount(); i++) {
            applyTabStyle(binding.tabContainer.getChildAt(i), i == currentPosition);
        }
    }

    @Override
    public void onDestroyView() {
        if (networkCallback != null) {
            try {
                ConnectivityManager connectivityManager =
                        (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }
            } catch (RuntimeException ignored) {
                // Already unregistered or the service is gone.
            }
            networkCallback = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
