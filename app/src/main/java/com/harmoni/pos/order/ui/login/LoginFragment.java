package com.harmoni.pos.order.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        MaterialButton loginButton = binding.loginButton;
        TextInputEditText usernameInput = binding.usernameInput;
        TextInputEditText passwordInput = binding.passwordInput;
        ProgressBar progressBar = binding.progressBar;
        TextView errorText = binding.errorText;

        loginButton.setOnClickListener(v ->
                viewModel.login(usernameInput.getText().toString(), passwordInput.getText().toString()));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!loading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                errorText.setText(message);
                errorText.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getLoggedIn().observe(getViewLifecycleOwner(), loggedIn -> {
            if (Boolean.TRUE.equals(loggedIn)) {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_login_to_orders);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
