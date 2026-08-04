package com.harmoni.pos.order.ui.reports;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.OrderVolume;
import com.harmoni.pos.order.databinding.FragmentDailyReportBinding;
import com.harmoni.pos.order.util.CurrencyUtils;
import com.harmoni.pos.order.util.TimeUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyReportFragment extends Fragment {

    private FragmentDailyReportBinding binding;
    private DailyReportViewModel viewModel;
    private DailyReportAdapter adapter;
    private String startDate;
    private String endDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDailyReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DailyReportViewModel.class);

        adapter = new DailyReportAdapter();
        binding.reportRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reportRecycler.setAdapter(adapter);

        startDate = TimeUtils.todayDate();
        endDate = TimeUtils.todayDate();
        binding.startDateButton.setText(startDate);
        binding.endDateButton.setText(endDate);

        binding.startDateButton.setOnClickListener(v -> pickDate(true));
        binding.endDateButton.setOnClickListener(v -> pickDate(false));
        binding.loadButton.setOnClickListener(v -> viewModel.load(startDate, endDate));
        binding.backButton.setOnClickListener(v -> Navigation.findNavController(requireView()).popBackStack());

        observe();
        viewModel.load(startDate, endDate);
    }

    private void observe() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            binding.errorText.setVisibility(message != null && !message.isEmpty() ? View.VISIBLE : View.GONE);
            if (message != null) binding.errorText.setText(message);
        });
        viewModel.getRows().observe(getViewLifecycleOwner(), this::onRowsLoaded);
        viewModel.getVolume().observe(getViewLifecycleOwner(), this::onVolumeLoaded);
    }

    private void onRowsLoaded(List<DailyReportViewModel.Row> rows) {
        adapter.submitList(rows);
        int totalQty = 0;
        double net = 0, discount = 0;
        for (DailyReportViewModel.Row row : rows) {
            totalQty += row.quantity;
            net += row.netSales;
            discount += row.discount;
        }
        binding.totalText.setText(String.format(Locale.US,
                "TOTAL   QTY: %d   Net: %s   Discount: %s",
                totalQty, CurrencyUtils.formatRp(net), CurrencyUtils.formatRp(discount)));
    }

    private void onVolumeLoaded(OrderVolume volume) {
        if (volume == null) return;
        binding.volumeText.setText(String.format(Locale.US,
                "Total Orders : %d    Peak Time : %d    Non Peak Time : %d",
                volume.getTotalOrders(), volume.getPeakTimeOrders(), volume.getNonPeakTimeOrders()));
    }

    private void pickDate(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        String current = isStart ? startDate : endDate;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(current);
            if (d != null) cal.setTime(d);
        } catch (ParseException ignored) {
        }
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (dp, year, month, dayOfMonth) -> {
                    String value = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    if (isStart) {
                        startDate = value;
                        binding.startDateButton.setText(value);
                    } else {
                        endDate = value;
                        binding.endDateButton.setText(value);
                    }
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
