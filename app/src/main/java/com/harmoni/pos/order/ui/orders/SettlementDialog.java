package com.harmoni.pos.order.ui.orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.Settlement;
import com.harmoni.pos.order.databinding.DialogSettlementBinding;
import com.harmoni.pos.order.util.CurrencyUtils;

import java.util.Map;

/** Structured settlement summary. Printing stays with the caller. */
public class SettlementDialog extends DialogFragment {

    private static final String ARG_DATA = "arg_settlement";

    public interface OnPrintListener {
        void onPrint(Settlement settlement);
    }

    private DialogSettlementBinding binding;
    private Settlement settlement;
    private OnPrintListener printListener;

    public static SettlementDialog newInstance(Settlement settlement) {
        SettlementDialog dialog = new SettlementDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATA, settlement);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnPrintListener(OnPrintListener listener) {
        this.printListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogSettlementBinding.inflate(inflater, container, false);
        return com.harmoni.pos.order.util.DialogFrame.wrap(inflater.getContext(), binding.getRoot());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            settlement = (Settlement) getArguments().getSerializable(ARG_DATA);
        }
        if (settlement == null) {
            dismiss();
            return;
        }

        binding.settlementDateText.setText(com.harmoni.pos.order.util.TimeUtils.todayDate());
        binding.totalOrdersText.setText(String.valueOf(settlement.getTotalOrders()));
        binding.totalSalesText.setText(CurrencyUtils.formatRp(SettlementDialog.this.settlement.getTotalSales()));
        binding.netSalesText.setText(CurrencyUtils.formatRp(SettlementDialog.this.settlement.getTotalNetSales()));
        binding.discountText.setText(CurrencyUtils.formatRp(SettlementDialog.this.settlement.getTotalDiscounts()));
        binding.taxText.setText(CurrencyUtils.formatRp(SettlementDialog.this.settlement.getTotalTax()));

        renderBreakdown(settlement.getPaymentBreakdown());

        binding.closeButton.setOnClickListener(v -> dismiss());
        binding.doneButton.setOnClickListener(v -> dismiss());
        binding.printButton.setOnClickListener(v -> {
            if (printListener != null) printListener.onPrint(settlement);
            dismiss();
        });
    }

    private void renderBreakdown(Map<String, Double> breakdown) {
        binding.breakdownContainer.removeAllViews();
        if (breakdown == null || breakdown.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.no_data);
            empty.setTextColor(requireContext().getColor(R.color.text_secondary));
            empty.setTextSize(13f);
            int pad = Math.round(14 * getResources().getDisplayMetrics().density);
            empty.setPadding(pad, pad, pad, pad);
            binding.breakdownContainer.addView(empty);
            return;
        }
        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = Math.round(14 * getResources().getDisplayMetrics().density);
            row.setPadding(pad, pad / 2, pad, pad / 2);

            TextView label = new TextView(requireContext());
            label.setText(entry.getKey());
            label.setTextColor(requireContext().getColor(R.color.text_primary));
            label.setTextSize(14f);
            label.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView amount = new TextView(requireContext());
            amount.setText(CurrencyUtils.formatRp(entry.getValue()));
            amount.setTextColor(requireContext().getColor(R.color.accent));
            amount.setTextSize(14f);
            amount.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(label);
            row.addView(amount);
            binding.breakdownContainer.addView(row);

            View divider = new View(requireContext());
            divider.setBackgroundColor(requireContext().getColor(R.color.divider));
            binding.breakdownContainer.addView(divider,
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null || getDialog().getWindow() == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        int width = Math.min((int) (screenWidth * 0.84f), (int) (880 * density));
        int height = Math.min((int) (screenHeight * 0.88f), (int) (780 * density));
        getDialog().getWindow().setLayout(width, height);
        com.harmoni.pos.order.util.DialogFrame.dimBehind(getDialog().getWindow(), 0.35f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
