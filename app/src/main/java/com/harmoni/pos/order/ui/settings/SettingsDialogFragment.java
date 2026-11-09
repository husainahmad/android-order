package com.harmoni.pos.order.ui.settings;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.data.remote.ConfigManager;
import com.harmoni.pos.order.data.remote.TokenManager;
import com.harmoni.pos.order.databinding.FragmentSettingsBinding;
import com.harmoni.pos.order.LocalizedActivity;
import com.harmoni.pos.order.print.PrinterManager;
import com.harmoni.pos.order.util.LanguageManager;

import androidx.navigation.Navigation;

import java.util.List;

public class SettingsDialogFragment extends DialogFragment {

    private FragmentSettingsBinding binding;

    private final ActivityResultLauncher<String> bluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding != null) {
                    if (granted) {
                        showBluetoothDevices();
                    } else {
                        setPrinterStatus(getString(R.string.bluetooth_permission_denied));
                        showBluetoothPermissionDeniedDialog(false);
                    }
                }
            });

    private String pendingPrintText;
    private final ActivityResultLauncher<String> bluetoothPrintPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingPrintText != null) {
                    doPrint(pendingPrintText);
                } else if (binding != null) {
                    setPrinterStatus(getString(R.string.bluetooth_permission_denied));
                    showBluetoothPermissionDeniedDialog(true);
                }
            });

    private void showBluetoothPermissionDeniedDialog(boolean forPrint) {
        boolean permanentlyDenied = !shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT);
        // On Android 12+ the permission appears as "Nearby devices" – not generic "Permissions" list.
        // If user sees "No permission requested" it means they are on Android <12 (no runtime needed)
        // or they haven't been prompted yet. Guide them correctly.
        String message = permanentlyDenied
                ? "Bluetooth permission was denied permanently.\n\nOn Android 12+ this appears as \"Nearby devices\" (not \"Permissions\").\n\nPlease enable it in:\nApp info → Permissions → Nearby devices → Allow"
                : "Bluetooth printing needs \"Nearby devices\" (BLUETOOTH_CONNECT) permission.\n\nPlease grant it to continue.";
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.bluetooth_permission_title))
                .setMessage(message)
                .setNegativeButton(getString(R.string.cancel), null);
        if (permanentlyDenied) {
            builder.setPositiveButton(getString(R.string.open_settings), (d, w) -> openAppSettings());
        } else {
            builder.setPositiveButton(getString(R.string.grant), (d, w) -> {
                if (forPrint) {
                    if (pendingPrintText != null) bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                } else {
                    bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                }
            });
            builder.setNeutralButton(getString(R.string.open_settings), (d, w) -> openAppSettings());
        }
        builder.show();
        // Also explain why Settings might say "No permission requested"
        if (binding != null && !PrinterManager.isBluetoothPermissionRequired()) {
            setPrinterStatus("Note: Android " + android.os.Build.VERSION.SDK_INT + " <31 does not require runtime Bluetooth permission. \"No permission requested\" is normal – printing should work. If it still fails, check Bluetooth is ON and printer is paired.");
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to generic settings
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return com.harmoni.pos.order.util.DialogFrame.wrap(inflater.getContext(), binding.getRoot());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.ThemeOverlay_OrderConfirm);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Custom Dialog to intercept touch outside EditText -> hide keyboard
        // Needed because MainActivity.dispatchTouchEvent() does NOT receive
        // touches from DialogFragment's separate Window.
        return new Dialog(requireContext(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View focused = getCurrentFocus();
                    if (focused instanceof EditText) {
                        Rect outRect = new Rect();
                        focused.getGlobalVisibleRect(outRect);
                        if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            focused.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getContext()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                            }
                        }
                    }
                }
                return super.dispatchTouchEvent(event);
            }
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.usernameText.setText(TokenManager.getUsername());
        binding.storeText.setText(getString(R.string.store_label, TokenManager.getStoreName()));
        binding.brandText.setText(getString(R.string.brand_label, TokenManager.getBrandName()));

        loadServerConfig();
        loadPrinterConfig();
        loadAppLanguage();
        setupActions();
        setupHideKeyboardOnTapOutside();
        updatePermissionStatus();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null || getDialog().getWindow() == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        int width = Math.min((int) (screenWidth * 0.86f), (int) (960 * density));
        int height = Math.min((int) (screenHeight * 0.90f), (int) (860 * density));
        getDialog().getWindow().setLayout(width, height);
        com.harmoni.pos.order.util.DialogFrame.dimBehind(getDialog().getWindow(), 0.35f);
    }

    private void loadServerConfig() {
        binding.serverHostInput.setText(ConfigManager.getHost());
        binding.serverPortInput.setText(String.valueOf(ConfigManager.getPort()));
    }

    private void loadAppLanguage() {
        String code = LanguageManager.get(requireContext());
        int checkedId;
        switch (code) {
            case "en": checkedId = R.id.appLangEn; break;
            case "in": checkedId = R.id.appLangIn; break;
            default: checkedId = R.id.appLangSystem; break;
        }
        binding.appLanguageGroup.check(checkedId);
        binding.appLanguageGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String selected = LanguageManager.SYSTEM;
            if (checkedIds.contains(R.id.appLangEn)) selected = "en";
            else if (checkedIds.contains(R.id.appLangIn)) selected = "in";
            if (selected.equals(LanguageManager.get(requireContext()))) return;
            LanguageManager.set(requireContext(), selected);
            // Rebuild the screen in the new language. The dialog itself closes so the
            // activity behind it is recreated against the new resources.
            if (getActivity() instanceof LocalizedActivity) {
                dismiss();
                ((LocalizedActivity) getActivity()).applyLanguageChange();
            }
        });
    }

    private void loadPrinterConfig() {
        String type = PrinterManager.getType();
        ChipGroup chipGroup = binding.printerTypeGroup;
        if (PrinterManager.TYPE_BLUETOOTH.equals(type)) {
            chipGroup.check(R.id.bluetoothChip);
        } else if (PrinterManager.TYPE_USB.equals(type)) {
            chipGroup.check(R.id.usbChip);
        } else {
            chipGroup.check(R.id.networkChip);
        }
        binding.printerAddressInput.setText(PrinterManager.getAddress());
        binding.printerPortInput.setText(String.valueOf(PrinterManager.getPort()));
        // Auto-fill MAC from connected/bonded Bluetooth if empty
        if (PrinterManager.TYPE_BLUETOOTH.equals(type)) {
            tryAutoFillBluetoothAddress();
        }
    }

    private void tryAutoFillBluetoothAddress() {
        String current = text(binding.printerAddressInput);
        if (current != null && !current.trim().isEmpty()) return;
        if (PrinterManager.isBluetoothPermissionRequired() && !PrinterManager.isBluetoothPermissionGranted()) return;
        List<BluetoothDevice> devices = PrinterManager.getBondedBluetoothDevices();
        if (devices.isEmpty()) return;
        // Try to find currently connected device via hidden isConnected(), else printer-like name, else first
        BluetoothDevice chosen = null;
        for (BluetoothDevice d : devices) {
            try {
                // Hidden API: isConnected() exists on Android 8+
                java.lang.reflect.Method m = d.getClass().getMethod("isConnected");
                boolean connected = (boolean) m.invoke(d);
                if (connected) { chosen = d; break; }
            } catch (Exception ignored) {}
        }
        if (chosen == null) {
            for (BluetoothDevice d : devices) {
                String name = d.getName();
                if (name != null) {
                    String n = name.toLowerCase();
                    if (n.contains("rpp") || n.contains("printer") || n.contains("pos") || n.contains("thermal") || n.contains("receipt")) {
                        chosen = d; break;
                    }
                }
            }
        }
        if (chosen == null) chosen = devices.get(0);
        String mac = chosen.getAddress();
        binding.printerAddressInput.setText(mac);
        // Auto-save so Test Print works without manual Save
        PrinterManager.save(PrinterManager.TYPE_BLUETOOTH, mac, parseInt(binding.printerPortInput, 9100));
        String displayName = chosen.getName() != null ? chosen.getName() : "Unknown";
        setPrinterStatus("Auto-filled Bluetooth printer: " + displayName + " (" + mac + ")");
    }

    private void updatePermissionStatus() {
        String status;
        int api = android.os.Build.VERSION.SDK_INT;
        String ver = android.os.Build.VERSION.RELEASE;
        if (!PrinterManager.isBluetoothPermissionRequired()) {
            status = "Android " + ver + " (API " + api + ") <12: Nearby devices does NOT exist. \"No permission requested\" is NORMAL. Bluetooth (normal) is auto-granted.";
        } else {
            boolean granted = PrinterManager.isBluetoothPermissionGranted();
            status = "Android " + ver + " (API " + api + ") 12+: Nearby devices = "
                    + (granted ? "Granted" : "Not granted")
                    + ". If \"No permission requested\" then tap Bluetooth Devices to trigger request, or check Settings → Apps → This app → Permissions → Nearby devices (on some OEMs: Additional permissions).";
        }
        if (binding != null) binding.printerStatusText.setText(status);
    }

    private String getBondedNamesForMessage() {
        try {
            List<BluetoothDevice> devices = PrinterManager.getBondedBluetoothDevices();
            if (devices.isEmpty()) return "none (please pair printer in Android Bluetooth first)";
            StringBuilder sb = new StringBuilder();
            for (BluetoothDevice d : devices) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(d.getName()).append(" (").append(d.getAddress()).append(")");
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void setupActions() {
        binding.backButton.setOnClickListener(v -> dismiss());
        binding.logoutButton.setOnClickListener(v -> {
            TokenManager.clearTokens();
            dismiss();
            Navigation.findNavController(requireView()).navigate(R.id.action_order_pager_to_login);
        });

        binding.serverSaveButton.setOnClickListener(v -> saveServer());
        binding.printerSaveButton.setOnClickListener(v -> savePrinter());
        binding.bluetoothDevicesButton.setOnClickListener(v -> onBluetoothClick());
        binding.usbPrinterButton.setOnClickListener(v -> onUsbClick());
        binding.testPrintButton.setOnClickListener(v -> onTestPrint());
        // Auto-fill MAC when user switches to Bluetooth
        binding.printerTypeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.bluetoothChip)) {
                // Defer to next frame so permission check has context
                binding.printerAddressInput.post(() -> tryAutoFillBluetoothAddress());
            }
        });
    }

    private void setupHideKeyboardOnTapOutside() {
        // UX: tapping anywhere outside an EditText should dismiss keyboard + clear focus.
        // The Dialog's dispatchTouchEvent() above handles the robust case (precise hit-test).
        // This adds a fallback so taps on empty container areas also clear focus reliably.
        View root = binding.getRoot();
        root.setClickable(true);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);

        // If user taps the scroll container / inner layout / cards, hide keyboard
        View.OnClickListener hide = v -> hideKeyboard();
        // Inner LinearLayout is the direct child of NestedScrollView
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            View inner = ((ViewGroup) root).getChildAt(0);
            inner.setClickable(true);
            inner.setOnClickListener(hide);
            // Fallback touch to handle quick taps where click may not fire
            inner.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) hideKeyboard();
                return false;
            });
        }
        // Extra safety: also hide when toolbar / status texts are tapped
        binding.settingsToolbar.setOnClickListener(hide);
    }

    private void hideKeyboard() {
        View focused = null;
        if (getDialog() != null) focused = getDialog().getCurrentFocus();
        if (focused == null && binding != null) focused = binding.getRoot().findFocus();
        if (focused instanceof EditText) {
            focused.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        } else {
            // No EditText focused but keyboard might still be visible
            View view = getView();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                // Move focus to root so next tap outside doesn't keep EditText focused
                binding.getRoot().requestFocus();
            }
        }
    }

    private void saveServer() {
        String host = text(binding.serverHostInput);
        int port = parseInt(binding.serverPortInput, 8080);
        ConfigManager.save(host, port);
        ApiClient.reset();
        binding.serverStatusText.setText(getString(R.string.server_saved, ConfigManager.getBaseOrder()));
    }

    private void savePrinter() {
        String type = currentPrinterType();
        String address = text(binding.printerAddressInput);
        int port = parseInt(binding.printerPortInput, 9100);
        PrinterManager.save(type, address, port);
        binding.printerStatusText.setText(getString(R.string.printer_saved, type));
    }

    private String currentPrinterType() {
        int id = binding.printerTypeGroup.getCheckedChipId();
        if (id == R.id.bluetoothChip) return PrinterManager.TYPE_BLUETOOTH;
        if (id == R.id.usbChip) return PrinterManager.TYPE_USB;
        return PrinterManager.TYPE_NETWORK;
    }

    private void onBluetoothClick() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.BLUETOOTH_CONNECT)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            return;
        }
        showBluetoothDevices();
    }

    private void showBluetoothDevices() {
        List<BluetoothDevice> devices = PrinterManager.getBondedBluetoothDevices();
        if (devices.isEmpty()) {
            setPrinterStatus(getString(R.string.no_bluetooth_devices));
            return;
        }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = devices.get(i).getName() + "\n" + devices.get(i).getAddress();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_bluetooth_printer))
                .setItems(names, (d, which) -> {
                    String address = devices.get(which).getAddress();
                    binding.printerAddressInput.setText(address);
                    binding.printerTypeGroup.check(R.id.bluetoothChip);
                    savePrinter();
                    setPrinterStatus(getString(R.string.bluetooth_printer_selected, address));
                })
                .show();
    }

    private void onUsbClick() {
        List<UsbDevice> devices = PrinterManager.getUsbPrinterDevices();
        if (devices.isEmpty()) {
            setPrinterStatus(getString(R.string.no_usb_printer));
            return;
        }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = PrinterManager.usbDeviceDescription(devices.get(i));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_usb_printer))
                .setItems(names, (d, which) -> {
                    UsbDevice device = devices.get(which);
                    PrinterManager.saveUsbDevice(PrinterManager.usbDeviceDescription(device));
                    binding.printerTypeGroup.check(R.id.usbChip);
                    binding.printerAddressInput.setText(PrinterManager.usbDeviceDescription(device));
                    savePrinter();
                    requestUsbThen(device, () ->
                            setPrinterStatus(getString(R.string.usb_printer_granted)));
                })
                .show();
    }

    private void requestUsbThen(UsbDevice device, Runnable onGranted) {
        if (PrinterManager.hasUsbPermission(device)) {
            onGranted.run();
            return;
        }
        PrinterManager.requestUsbPermission(requireActivity(), device,
                onGranted,
                () -> setPrinterStatus(getString(R.string.usb_permission_denied)));
    }

    private void onTestPrint() {
        savePrinter();
        String testText = "KOPI HARMONI\n"
                + "----------------------------\n"
                + "TEST PRINT\n"
                + "Printer: " + currentPrinterType() + "\n"
                + "----------------------------\n"
                + "Terima kasih\n";
        String type = currentPrinterType();
        // Auto-request Bluetooth permission before printing
        if (PrinterManager.TYPE_BLUETOOTH.equals(type) && !PrinterManager.isBluetoothPermissionGranted()) {
            pendingPrintText = testText;
            if (PrinterManager.isBluetoothPermissionRequired()) {
                bluetoothPrintPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                setPrinterStatus("Requesting Bluetooth permission…");
                return;
            }
        }
        // Guard: Bluetooth selected but no MAC saved → try auto-fill from connected device first
        if (PrinterManager.TYPE_BLUETOOTH.equals(type)) {
            String mac = PrinterManager.getAddress();
            if (mac == null || mac.trim().isEmpty()) {
                // Try to auto-fill from bonded/connected device before prompting
                tryAutoFillBluetoothAddress();
                mac = PrinterManager.getAddress();
                // Also check the EditText (may have been auto-filled but not yet saved)
                String inputMac = text(binding.printerAddressInput);
                if (mac == null || mac.trim().isEmpty()) mac = inputMac;
            }
            if (mac == null || mac.trim().isEmpty()) {
                setPrinterStatus(getString(R.string.bluetooth_printer_not_selected));
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.bluetooth_printer_not_selected))
                        .setMessage("No Bluetooth MAC saved.\n\nTap \"Bluetooth Devices\" to pick a paired printer (ensure printer is paired in Android Settings → Bluetooth), then tap Test Print again.\n\nNearby bonded: " + getBondedNamesForMessage())
                        .setPositiveButton(getString(R.string.bluetooth_devices), (d, w) -> onBluetoothClick())
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                return;
            }
        }
        if (PrinterManager.TYPE_USB.equals(type)) {
            UsbDevice device = null;
            for (UsbDevice d : PrinterManager.getUsbPrinterDevices()) {
                if (PrinterManager.hasUsbPermission(d)) {
                    device = d;
                    break;
                }
            }
            if (device == null) {
                List<UsbDevice> devices = PrinterManager.getUsbPrinterDevices();
                if (!devices.isEmpty()) {
                    requestUsbThen(devices.get(0), () -> doPrint(testText));
                    return;
                }
            }
        }
        doPrint(testText);
    }

    private void doPrint(String text) {
        setPrinterStatus(getString(R.string.printing));
        PrinterManager.print(text, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                setPrinterStatus(getString(R.string.print_sent));
            }

            @Override
            public void onError(String message) {
                setPrinterStatus(getString(R.string.print_failed, message));
                if (message != null && message.toLowerCase().contains("not selected")) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.bluetooth_printer_not_selected))
                            .setMessage(message + "\n\nGo to Bluetooth Devices to select a paired printer.")
                            .setPositiveButton(getString(R.string.bluetooth_devices), (d, w) -> onBluetoothClick())
                            .setNegativeButton(getString(R.string.close), null)
                            .show();
                } else if (message != null && message.toLowerCase().contains("bluetooth is off")) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.bluetooth_is_off))
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.open_bluetooth_settings), (d, w) -> {
                                try { startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)); } catch (Exception ignored) {}
                            })
                            .setNegativeButton(getString(R.string.close), null)
                            .show();
                }
            }
        });
    }

    private void setPrinterStatus(String message) {
        if (binding != null) {
            binding.printerStatusText.setText(message);
        }
    }

    private static String text(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static int parseInt(EditText input, int def) {
        try {
            String s = text(input);
            return s.isEmpty() ? def : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}