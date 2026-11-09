package com.harmoni.pos.order.ui.orderform;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.model.CreateCustomerRequest;
import com.harmoni.pos.order.data.model.Customer;
import com.harmoni.pos.order.data.model.CustomerPage;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.databinding.DialogCustomerPickerBinding;
import com.harmoni.pos.order.util.PhoneUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search an existing customer or register a new one, then hand the choice back to the
 * order form. A walk-in is the absence of a customer and is signalled separately.
 */
public class CustomerPickerDialogFragment extends DialogFragment {

    public interface OnCustomerListener {
        void onSelect(Customer customer);
        void onClear();
    }

    private DialogCustomerPickerBinding binding;
    private CustomerAdapter adapter;
    private OnCustomerListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    public void setOnCustomerListener(OnCustomerListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogCustomerPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new CustomerAdapter(customer -> {
            if (listener != null) listener.onSelect(customer);
            dismiss();
        });
        binding.customerRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.customerRecycler.setHasFixedSize(true);
        binding.customerRecycler.setAdapter(adapter);

        binding.closeButton.setOnClickListener(v -> dismiss());
        binding.walkInButton.setOnClickListener(v -> {
            if (listener != null) listener.onClear();
            dismiss();
        });
        binding.registerButton.setOnClickListener(v -> register());

        binding.customerSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { scheduleSearch(s.toString()); }
        });

        showStatus(getString(R.string.customer_search_prompt));
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            int maxWidth = (int) (640 * getResources().getDisplayMetrics().density);
            if (width > maxWidth) width = maxWidth;
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setLayout(width, height);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void scheduleSearch(String text) {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        final String query = text == null ? "" : text.trim();
        if (query.length() < 2) {
            showStatus(getString(R.string.customer_search_prompt));
            adapter.submitList(null);
            return;
        }
        pendingSearch = () -> search(query);
        handler.postDelayed(pendingSearch, 300);
    }

    private void search(String query) {
        if (binding == null) return;
        showStatus("Searching...");
        ApiClient.customerService().searchCustomers(query, 0, 20)
                .enqueue(new Callback<CustomerPage>() {
                    @Override
                    public void onResponse(Call<CustomerPage> call, Response<CustomerPage> response) {
                        if (binding == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.submitList(response.body().getContent());
                            if (response.body().getContent().isEmpty()) {
                                showStatus(getString(R.string.customer_no_results));
                            } else {
                                hideStatus();
                            }
                        } else {
                            showStatus("HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<CustomerPage> call, Throwable t) {
                        if (binding != null) showStatus("Network error: " + t.getMessage());
                    }
                });
    }

    private void register() {
        String name = text(binding.newCustomerNameInput);
        String phone = text(binding.newCustomerPhoneInput);
        String email = text(binding.newCustomerEmailInput);

        if (name.isEmpty()) {
            showStatus(getString(R.string.customer_name_required));
            return;
        }
        if (!phone.isEmpty() && !PhoneUtils.isValid(phone)) {
            showStatus(getString(R.string.customer_phone_invalid));
            return;
        }
        showStatus("Registering...");
        binding.registerButton.setEnabled(false);
        ApiClient.customerService().createCustomer(
                new CreateCustomerRequest(name, phone, email.isEmpty() ? null : email))
                .enqueue(new Callback<Customer>() {
                    @Override
                    public void onResponse(Call<Customer> call, Response<Customer> response) {
                        if (binding == null) return;
                        binding.registerButton.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            if (listener != null) listener.onSelect(response.body());
                            dismiss();
                        } else {
                            showStatus(getString(R.string.customer_register_failed)
                                    + " (HTTP " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<Customer> call, Throwable t) {
                        if (binding == null) return;
                        binding.registerButton.setEnabled(true);
                        showStatus("Network error: " + t.getMessage());
                    }
                });
    }

    private void showStatus(String message) {
        binding.statusText.setText(message);
        binding.statusText.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        binding.statusText.setVisibility(View.GONE);
    }

    private static String text(android.widget.EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        binding = null;
    }
}
