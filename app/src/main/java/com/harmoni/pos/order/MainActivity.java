package com.harmoni.pos.order;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.harmoni.pos.order.databinding.ActivityMainBinding;
import com.harmoni.pos.order.print.PrinterManager;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> bluetoothPermissionLauncher;
    private ActivityResultLauncher<String[]> bluetoothMultipleLauncher;

    private void initBluetoothLauncher() {
        bluetoothPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                Toast.makeText(this, "Bluetooth permission granted – printing enabled", Toast.LENGTH_SHORT).show();
            } else {
                boolean permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT);
                String msg = permanentlyDenied
                        ? "Bluetooth permission was denied permanently.\n\nOn Android 12+ this is \"Nearby devices\".\nEnable in: App info → Permissions → Nearby devices → Allow\n\nIf you don't see \"Nearby devices\", your Android version is <12 – see note below."
                        : "Bluetooth permission denied.\nOn Android 12+ this is \"Nearby devices\" (BLUETOOTH_CONNECT).";
                com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Bluetooth Permission Required")
                            .setMessage(msg + "\n\nDevice: Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")"
                                    + "\nRequired: " + (PrinterManager.isBluetoothPermissionRequired() ? "Yes (12+)" : "No (<12 normal)"))
                            .setNegativeButton("Cancel", null);
                if (permanentlyDenied) {
                    builder.setPositiveButton("Open Settings", (d, w) -> openAppSettings());
                } else {
                    builder.setPositiveButton("Try Again", (d, w) -> {
                        if (bluetoothPermissionLauncher != null) bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                    });
                    builder.setNeutralButton("Open Settings", (d, w) -> openAppSettings());
                }
                builder.show();
                if (!PrinterManager.isBluetoothPermissionRequired()) {
                    Toast.makeText(this, "This Android " + android.os.Build.VERSION.RELEASE + " (<12) shows \"No permission requested\" – this is NORMAL. Legacy Bluetooth (normal) is auto-granted, printing should work without Nearby devices.", Toast.LENGTH_LONG).show();
                }
            }
        });
        bluetoothMultipleLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean c = result.get(Manifest.permission.BLUETOOTH_CONNECT);
            boolean granted = c != null && c;
            if (granted) {
                Toast.makeText(this, "Bluetooth permission granted – printing enabled", Toast.LENGTH_SHORT).show();
            } else {
                // reuse same dialog
                boolean permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT);
                String msg = permanentlyDenied
                        ? "Bluetooth permission was denied permanently.\n\nOn Android 12+ this is \"Nearby devices\".\nEnable in: App info → Permissions → Nearby devices → Allow"
                        : "Bluetooth permission denied.";
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Bluetooth Permission Required")
                        .setMessage(msg)
                        .setPositiveButton("Open Settings", (d, w) -> openAppSettings())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBluetoothLauncher();
        enableEdgeToEdge();
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Fix transparent nav bar overlapping bottom buttons (Daily Report etc.)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });
        setupExitConfirmation();
        ensureBluetoothPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check when returning from Settings – if user switched to Bluetooth printer
        ensureBluetoothPermissionIfNeeded();
    }

    private void ensureBluetoothPermissionIfNeeded() {
        if (!PrinterManager.isBluetoothPermissionRequired()) {
            // On Android 8-11 (API 26-30) Nearby devices doesn't exist → "No permission requested" is NORMAL
            return;
        }
        if (PrinterManager.isBluetoothPermissionGranted()) return;
        boolean permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT);
        android.content.SharedPreferences sp = getSharedPreferences("perm_prefs", MODE_PRIVATE);
        boolean alreadyRequested = sp.getBoolean("bluetooth_requested", false);
        if (permanentlyDenied && alreadyRequested) {
            return;
        }
        sp.edit().putBoolean("bluetooth_requested", true).apply();
        // Request both CONNECT and SCAN so the Nearby devices group is created reliably on some OEMs
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                bluetoothMultipleLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                return;
            } catch (Exception ignored) {}
        }
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
    }



    private void setupExitConfirmation() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Only confirm when at start destination (no fragment back stack)
                androidx.navigation.NavController navController =
                        androidx.navigation.Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() != R.id.nav_order_pager
                        && navController.getCurrentDestination().getId() != R.id.nav_login) {
                    // Let NavController handle inner back stack / dialogs
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                    return;
                }
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage("Are you sure you want to exit the app?")
                        .setPositiveButton(R.string.close, (d, w) -> finish())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    private void enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // Opaque dark nav bar for contrast: white icons on dark background (fixes white-on-white)
        getWindow().setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.md_inverse_surface));
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightNavigationBars(false); // white icons on dark bar
            controller.setAppearanceLightStatusBars(true); // dark icons on light status bar
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
