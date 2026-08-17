package com.harmoni.pos.order.ui.orderpager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.databinding.FragmentOrderPagerBinding;
import com.harmoni.pos.order.ui.orderform.OrderFormViewModel;
import com.harmoni.pos.order.ui.orders.OrdersViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderPagerFragment extends Fragment {

    private FragmentOrderPagerBinding binding;
    private OrderFormViewModel viewModel;
    private OrdersViewModel ordersViewModel;
    private OrderPagerAdapter pagerAdapter;

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

        com.harmoni.pos.order.util.UiUtils.applyStatusBarTopInset(binding.tabScrollView);

        viewModel = new ViewModelProvider(requireActivity()).get(OrderFormViewModel.class);
        ordersViewModel = new ViewModelProvider(requireActivity()).get(OrdersViewModel.class);
        pagerAdapter = new OrderPagerAdapter(requireActivity());

        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setOffscreenPageLimit(10);

        ordersViewModel.loadOrders();

        viewModel.getTabs().observe(getViewLifecycleOwner(), tabs -> {
            List<Integer> ids = new ArrayList<>();
            for (OrderFormViewModel.OrderTab tab : tabs) {
                ids.add(tab.id);
            }
            pagerAdapter.setOrderIds(ids);
            renderHeaderTabs(tabs);
            if (ids.isEmpty()) {
                binding.viewPager.post(() -> {
                    if (binding.viewPager.getCurrentItem() != 0) {
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

    private String lastHeaderSignature;

    private void renderHeaderTabs(List<OrderFormViewModel.OrderTab> tabs) {
        if (tabs == null) {
            return;
        }
        StringBuilder sig = new StringBuilder();
        for (OrderFormViewModel.OrderTab tab : tabs) {
            sig.append(tab.id).append(':').append(tab.label).append(';');
        }
        String signature = sig.toString();
        if (signature.equals(lastHeaderSignature)) {
            updateTabSelection(binding.viewPager.getCurrentItem());
            return;
        }
        lastHeaderSignature = signature;
        binding.tabContainer.removeAllViews();
        int currentPosition = binding.viewPager.getCurrentItem();

        addTabHeaderView(getString(R.string.orders), 0, -1, currentPosition == 0, false);
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
        chip.setElevation(selected ? 4f : 1f);
        chip.setBackgroundResource(selected ? R.drawable.bg_order_tab_selected
                : R.drawable.bg_order_tab);
        TextView label = chip.findViewById(R.id.tabLabel);
        label.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.white : R.color.text_primary));
        ImageButton close = chip.findViewById(R.id.closeButton);
        close.setImageTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        selected ? R.color.white : R.color.text_primary)));
    }

    private void addTabHeaderView(String text, int targetPosition, int orderId, boolean isSelected, boolean isCloseable) {
        LinearLayout chip = (LinearLayout) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_order_tab, binding.tabContainer, false);
        TextView label = chip.findViewById(R.id.tabLabel);
        ImageButton close = chip.findViewById(R.id.closeButton);

        label.setText(text);
        close.setVisibility(isCloseable ? View.VISIBLE : View.GONE);

        applyTabStyle(chip, isSelected);

        if (isCloseable) {
            close.setOnClickListener(v -> confirmAndRemoveTab(orderId));
        }
        chip.setOnClickListener(v -> {
            if (targetPosition == -1) {
                int newOrderId = viewModel.createNewTab();
                binding.viewPager.post(() -> {
                    int pos = pagerAdapter.getPositionForOrderId(newOrderId);
                    if (pos != -1) {
                        binding.viewPager.setCurrentItem(pos, true);
                    }
                });
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
        super.onDestroyView();
        binding = null;
    }
}
