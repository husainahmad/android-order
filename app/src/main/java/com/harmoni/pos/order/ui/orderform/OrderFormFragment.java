package com.harmoni.pos.order.ui.orderform;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CartItem;
import com.harmoni.pos.order.data.model.Category;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.data.repository.MenuRepository;
import com.harmoni.pos.order.databinding.FragmentOrderFormBinding;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.harmoni.pos.order.util.PrintUtils;

public class OrderFormFragment extends Fragment {

    private static final String ARG_TAB_ID = "arg_tab_id";

    public static OrderFormFragment newInstance(int tabId) {
        OrderFormFragment fragment = new OrderFormFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_ID, tabId);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentOrderFormBinding binding;
    private OrderFormViewModel viewModel;
    private MenuRepository menuRepository;
    private int tabId;
    private ProductAdapter productAdapter;
    private CartAdapter cartAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
    private int selectedCategoryId = -1;
    private boolean productsInitiallyLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            tabId = getArguments().getInt(ARG_TAB_ID, -1);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(OrderFormViewModel.class);
        menuRepository = new MenuRepository();

        setupRecyclers();
        setupActions();
        observe();
        viewModel.ensureActiveTab();
        loadCategories();
    }

    private void setupRecyclers() {
        productAdapter = new ProductAdapter(this::onProductClick);
        RecyclerView productsRecycler = binding.productsRecycler;
        productsRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        productsRecycler.setAdapter(productAdapter);

        categoryAdapter = new CategoryAdapter(category -> selectCategory(category));
        RecyclerView categoryRecycler = binding.categoryRecycler;
        categoryRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryRecycler.setAdapter(categoryAdapter);

        cartAdapter = new CartAdapter(new CartAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrement(int productId, int skuId) {
                viewModel.increment(productId, skuId);
            }

            @Override
            public void onDecrement(int productId, int skuId) {
                viewModel.decrement(productId, skuId);
            }
        });
        binding.cartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.cartRecycler.setAdapter(cartAdapter);
    }

    private void setupActions() {
        binding.confirmButton.setOnClickListener(v -> onConfirm());
        binding.printButton.setOnClickListener(v -> onPrint());
    }

    private void observe() {
        viewModel.getActiveTabId().observe(getViewLifecycleOwner(), id -> updateTotals());
        viewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            cartAdapter.submitList(cart);
            boolean empty = cart == null || cart.isEmpty();
            binding.cartRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.cartEmptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) {
                binding.cartEmptyView.emptyIcon.setImageResource(R.drawable.ic_empty_cart);
                binding.cartEmptyView.emptyTitle.setText(R.string.cart_empty);
                binding.cartEmptyView.emptySubtitle.setText(R.string.cart_empty_subtitle);
            }
            updateTotals();
        });
        viewModel.getSubmitting().observe(getViewLifecycleOwner(), submitting ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(submitting) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isEmpty()) {
                binding.errorText.setVisibility(View.GONE);
            } else {
                binding.errorText.setText(message);
                binding.errorText.setVisibility(View.VISIBLE);
            }
        });
        viewModel.getConfirmedOrder().observe(getViewLifecycleOwner(), order -> {
        });
    }

    private void loadCategories() {
        binding.progressBar.setVisibility(View.VISIBLE);
        menuRepository.getCategories(new MenuRepository.RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> data) {
                categories = data;
                categoryAdapter.submitList(data);
                binding.progressBar.setVisibility(View.GONE);
                if (!data.isEmpty()) {
                    selectCategory(data.get(0));
                }
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    private void selectCategory(Category category) {
        selectedCategoryId = category.getId();
        binding.categoryTitleText.setText(category.getName());
        loadProducts(category.getId());
    }

    private void loadProducts(int categoryId) {
        binding.progressBar.setVisibility(View.VISIBLE);
        menuRepository.getProducts(categoryId, new MenuRepository.RepositoryCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> data) {
                allProducts = data;
                productAdapter.submitList(allProducts);
                binding.progressBar.setVisibility(View.GONE);
                boolean empty = data == null || data.isEmpty();
                binding.productsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.productsEmptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) {
                    binding.productsEmptyView.emptyIcon.setImageResource(R.drawable.ic_empty_products);
                    binding.productsEmptyView.emptyTitle.setText(R.string.no_products);
                    binding.productsEmptyView.emptySubtitle.setText(R.string.no_products_subtitle);
                }
                if (!productsInitiallyLoaded) {
                    productsInitiallyLoaded = true;
                    animateProductsIn();
                }
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    private void animateProductsIn() {
        android.view.animation.LayoutAnimationController controller =
                android.view.animation.AnimationUtils.loadLayoutAnimation(
                        requireContext(), R.anim.layout_products);
        binding.productsRecycler.setLayoutAnimation(controller);
        binding.productsRecycler.scheduleLayoutAnimation();
    }

    private void onProductClick(Product product) {
        List<Sku> skus = product.getSkus();
        if (skus != null && skus.size() == 1 && skus.get(0) != null) {
            viewModel.addToCart(product, skus.get(0));
            return;
        }
        ProductDetailDialog dialog = new ProductDetailDialog();
        dialog.setProduct(product);
        dialog.setOnSkuListener(new ProductDetailDialog.OnSkuListener() {
            @Override
            public void onAdd(Product p, Sku s) {
                viewModel.addToCart(p, s);
            }

            @Override
            public void onRemove(Product p, Sku s) {
                viewModel.decrement(p.getId(), s.getId());
            }
        });
        dialog.show(getParentFragmentManager(), "product_detail");
    }

    private void updateTotals() {
        double subtotal = viewModel.getSubtotal();
        binding.subtotalText.setText(CurrencyUtils.formatRp(subtotal));
        binding.totalText.setText(CurrencyUtils.formatRp(subtotal));
    }

    private void onConfirm() {
        List<CartItem> cart = viewModel.getCart().getValue();
        if (cart == null || cart.isEmpty()) {
            showError(getString(R.string.cart_empty));
            return;
        }
        new OrderConfirmFragment()
                .show(getParentFragmentManager(), "order_confirm");
    }

    private void onPrint() {
        List<CartItem> cart = viewModel.getCart().getValue();
        if (cart == null || cart.isEmpty()) {
            showError(getString(R.string.cart_empty));
            return;
        }
        String kitchenText = buildKitchenText(cart);
        Toast.makeText(requireContext(), R.string.kitchen_print + "...", Toast.LENGTH_SHORT).show();
        PrinterManager.print(kitchenText, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), R.string.kitchen_print + " sent",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "Print failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String buildKitchenText(List<CartItem> cart) {
        StringBuilder sb = new StringBuilder();
        sb.append("KOPI HARMONI - KP\n");
        sb.append("----------------------------\n");
        OrderFormViewModel.OrderTab tab = viewModel.getActiveTab();
        String customer = tab != null ? tab.customer : "";
        sb.append("Cust : ").append(customer).append("\n\n");
        Map<Integer, List<CartItem>> groups = new LinkedHashMap<>();
        for (CartItem item : cart) {
            groups.computeIfAbsent(item.getProductId(), k -> new ArrayList<>()).add(item);
        }
        for (List<CartItem> group : groups.values()) {
            sb.append(group.get(0).getProductName()).append("\n");
            for (CartItem item : group) {
                sb.append("  ").append(item.getSkuName()).append(" x ")
                        .append(item.getQuantity()).append("\n");
            }
        }
        sb.append("----------------------------\n");
        return sb.toString();
    }

    private void showError(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
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
