package com.harmoni.pos.order.ui.orderform;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CustomizationOption;
import com.harmoni.pos.order.data.model.CustomizationSelection;
import com.harmoni.pos.order.data.model.Product;
import com.harmoni.pos.order.data.model.ProductCustomization;
import com.harmoni.pos.order.data.model.SelectedCustomization;
import com.harmoni.pos.order.data.model.Sku;
import com.harmoni.pos.order.databinding.DialogProductDetailBinding;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * One dialog for the whole line, styled like the POS mockup: the product header, a
 * variant chip row, the customization groups (chips when one answer, checkbox cards
 * when several), a kitchen note, and a footer with the line quantity and a single
 * "Add to order" button showing the line total.
 */
public class ProductDetailDialog extends BottomSheetDialogFragment {

    public interface OnSkuListener {
        void onAdd(Product product, Sku sku, List<SelectedCustomization> customizations,
                   int quantity, String note);
    }

    private DialogProductDetailBinding binding;
    private Product product;
    private List<ProductCustomization> customizations = new ArrayList<>();
    private OnSkuListener listener;
    private String initialNote = "";

    private Sku selectedSku;
    private final CustomizationSelection selection = new CustomizationSelection();
    private int quantity = 1;

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setCustomizations(List<ProductCustomization> customizations) {
        this.customizations = customizations == null ? new ArrayList<>() : customizations;
    }

    public void setInitialNote(String note) {
        this.initialNote = note == null ? "" : note;
    }

    public void setOnSkuListener(OnSkuListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.productNameText.setText(product.getName());
        binding.closeButton.setOnClickListener(v -> dismiss());
        binding.noteInput.setText(initialNote);

        List<Sku> skus = product.getSkus();
        boolean multipleSkus = skus != null && skus.size() > 1;
        if (!multipleSkus && skus != null && skus.size() == 1) {
            selectedSku = skus.get(0);
        }
        binding.variantTitleText.setVisibility(multipleSkus ? View.VISIBLE : View.GONE);
        binding.skuContainer.setVisibility(multipleSkus ? View.VISIBLE : View.GONE);
        binding.variantTitleText.setText(R.string.select_variant);

        binding.minusButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantity();
            }
        });
        binding.plusButton.setOnClickListener(v -> {
            quantity++;
            updateQuantity();
        });
        binding.addButton.setOnClickListener(v -> confirmAdd());

        renderVariants();
        renderCustomizations();
        updateQuantity();
        updateHeaderSubtitle();
        updateAddState();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) return;
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        // Show the full option list on a tablet without turning into a full-screen dialog.
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.86);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = height;
        bottomSheet.setLayoutParams(params);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }

    // MARK: - Variant chips

    private void renderVariants() {
        binding.skuContainer.removeAllViews();
        List<Sku> skus = product.getSkus();
        if (skus == null || skus.isEmpty()) return;
        ChipGroup group = new ChipGroup(requireContext());
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(dp(8));
        group.setChipSpacingVertical(dp(8));
        for (Sku sku : skus) {
            Chip chip = buildChip(sku.getName() + " · " + CurrencyUtils.formatRp(sku.getPrice()));
            chip.setChecked(selectedSku != null && selectedSku.getId() == sku.getId());
            chip.setOnClickListener(v -> {
                selectedSku = sku;
                // Options are chosen per variant, so a different variant starts fresh.
                selection.clear();
                renderVariants();
                renderCustomizations();
                updateHeaderSubtitle();
                updateAddState();
            });
            group.addView(chip);
        }
        binding.skuContainer.addView(group);
    }

    // MARK: - Customization groups

    private void renderCustomizations() {
        binding.customizationContainer.removeAllViews();
        if (customizations.isEmpty()) return;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ProductCustomization group : customizations) {
            View groupView = inflater.inflate(R.layout.item_customization_group,
                    binding.customizationContainer, false);
            ((TextView) groupView.findViewById(R.id.groupNameText)).setText(group.getName());
            ((TextView) groupView.findViewById(R.id.groupHintText)).setText(groupHint(group));
            TextView description = groupView.findViewById(R.id.groupDescriptionText);
            if (!group.getDescription().isEmpty()) {
                description.setText(group.getDescription());
                description.setVisibility(View.VISIBLE);
            }

            ChipGroup chipContainer = groupView.findViewById(R.id.chipContainer);
            LinearLayout optionContainer = groupView.findViewById(R.id.optionContainer);

            if (group.allowsMultiple()) {
                optionContainer.setVisibility(View.VISIBLE);
                buildOptionGrid(optionContainer, group);
            } else {
                chipContainer.setVisibility(View.VISIBLE);
                buildOptionChips(chipContainer, group);
            }
            binding.customizationContainer.addView(groupView);
        }
    }

    private void buildOptionChips(ChipGroup container, ProductCustomization group) {
        for (CustomizationOption option : group.getSortedOptions()) {
            String label = option.getName();
            double price = option.getPrice();
            if (price > 0) label += " · +" + CurrencyUtils.formatRp(price);
            Chip chip = buildChip(label);
            chip.setChecked(selection.isSelected(option.getId(), group.getCustomizationId()));
            chip.setOnClickListener(v -> {
                selection.select(option.getId(), group);
                renderCustomizations();
                updateAddState();
            });
            container.addView(chip);
        }
    }

    private void buildOptionGrid(LinearLayout container, ProductCustomization group) {
        List<CustomizationOption> options = group.getSortedOptions();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        LinearLayout row = null;
        for (int i = 0; i < options.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = dp(8);
                row.setLayoutParams(rp);
                container.addView(row);
            }
            View card = inflater.inflate(R.layout.item_customization_option_card, row, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) lp.rightMargin = dp(8);
            card.setLayoutParams(lp);
            bindOptionCard(card, group, options.get(i));
            row.addView(card);
        }
    }

    private void bindOptionCard(View card, ProductCustomization group, CustomizationOption option) {
        ImageView check = card.findViewById(R.id.optionCheck);
        TextView name = card.findViewById(R.id.optionNameText);
        TextView price = card.findViewById(R.id.optionPriceText);
        LinearLayout qtyContainer = card.findViewById(R.id.optionQtyContainer);
        MaterialButton minus = card.findViewById(R.id.optionMinusButton);
        MaterialButton plus = card.findViewById(R.id.optionPlusButton);
        TextView qtyText = card.findViewById(R.id.optionQuantityText);

        int optionId = option.getId();
        int customizationId = group.getCustomizationId();
        boolean selected = selection.isSelected(optionId, customizationId);

        name.setText(option.getName());
        double optionPrice = option.getPrice();
        price.setText(optionPrice > 0 ? "+" + CurrencyUtils.formatRp(optionPrice) : getString(R.string.free));
        check.setImageResource(selected ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline);
        card.setSelected(selected);

        if (group.allowsQuantity()) {
            qtyContainer.setVisibility(selected ? View.VISIBLE : View.GONE);
            qtyText.setText(String.valueOf(Math.max(1, selection.count(optionId, customizationId))));
            plus.setEnabled(selection.canIncrement(optionId, group));
        } else {
            qtyContainer.setVisibility(View.GONE);
        }

        card.setOnClickListener(v -> {
            selection.select(optionId, group);
            renderCustomizations();
            updateAddState();
        });
        minus.setOnClickListener(v -> {
            selection.decrement(optionId, group);
            renderCustomizations();
            updateAddState();
        });
        plus.setOnClickListener(v -> {
            selection.increment(optionId, group);
            renderCustomizations();
            updateAddState();
        });
    }

    private Chip buildChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setTextColor(getResources().getColorStateList(R.color.option_chip_text, null));
        chip.setChipBackgroundColor(getResources().getColorStateList(R.color.option_chip_bg, null));
        chip.setChipStrokeColor(getResources().getColorStateList(R.color.option_chip_stroke, null));
        chip.setChipStrokeWidth(dp(1));
        chip.setEnsureMinTouchTargetSize(true);
        chip.setMinHeight(dp(44));
        chip.setChipCornerRadius(dp(24));
        return chip;
    }

    private String groupHint(ProductCustomization group) {
        int min = group.getMinCount();
        int max = group.getMaxCount();
        if (min > 0 && group.allowsMultiple() && max > min) {
            return getString(R.string.group_hint_choose_range, min, max);
        }
        if (min > 0) return getString(R.string.group_hint_choose_one);
        if (group.allowsMultiple() && max > 1) {
            return getString(R.string.group_hint_choose_up_to, max);
        }
        return getString(R.string.group_hint_optional);
    }

    // MARK: - Footer / state

    private void updateQuantity() {
        binding.quantityText.setText(String.valueOf(quantity));
        binding.minusButton.setEnabled(quantity > 1);
        updateAddState();
    }

    private void updateHeaderSubtitle() {
        double price = selectedSku != null ? selectedSku.getPrice() : defaultPrice();
        String subtitle = CurrencyUtils.formatRp(price);
        if (selectedSku != null && product.getSkus() != null && product.getSkus().size() > 1) {
            subtitle += " · " + selectedSku.getName();
        }
        binding.productSubtitleText.setText(subtitle);
    }

    private double defaultPrice() {
        List<Sku> skus = product.getSkus();
        return skus == null || skus.isEmpty() ? 0 : skus.get(0).getPrice();
    }

    private double lineTotal() {
        if (selectedSku == null) return 0;
        double surcharge = 0;
        for (SelectedCustomization c : selection.resolved(customizations)) {
            surcharge += c.getPrice() * Math.max(1, c.getQuantity());
        }
        return (selectedSku.getPrice() + surcharge) * quantity;
    }

    private void updateAddState() {
        boolean ready = selectedSku != null
                && selection.unmetRequirements(customizations).isEmpty();
        binding.addButton.setEnabled(ready);
        binding.addButton.setAlpha(ready ? 1f : 0.5f);
        binding.addButton.setText(getString(R.string.add_to_order)
                + (ready ? " · " + CurrencyUtils.formatRp(lineTotal()) : ""));
    }

    private void confirmAdd() {
        if (selectedSku == null) return;
        List<ProductCustomization> unmet = selection.unmetRequirements(customizations);
        if (!unmet.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ProductCustomization group : unmet) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(group.getName());
            }
            binding.addButton.setText(getString(R.string.group_hint_required) + ": " + sb);
            return;
        }
        String note = binding.noteInput.getText() == null
                ? "" : binding.noteInput.getText().toString().trim();
        if (listener != null) {
            listener.onAdd(product, selectedSku, selection.resolved(customizations), quantity, note);
        }
        dismiss();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
