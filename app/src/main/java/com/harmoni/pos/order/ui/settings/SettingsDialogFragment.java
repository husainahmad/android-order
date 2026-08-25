package com.harmoni.pos.order.ui.settings;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harmoni.pos.order.R;
import com.harmoni.pos.order.data.remote.ApiClient;
import com.harmoni.pos.order.data.remote.ConfigManager;
import com.harmoni.pos.order.data.remote.TokenManager;
import com.harmoni.pos.order.databinding.FragmentSettingsBinding;
import com.harmoni.pos.order.print.PrinterManager;

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
                        setPrinterStatus("Bluetooth permission denied");
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.ThemeOverlay_OrderConfirm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.usernameText.setText(TokenManager.getUsername());
        binding.storeText.setText("Store : " + TokenManager.getStoreName());
        binding.brandText.setText("Brand : " + TokenManager.getBrandName());

        loadServerConfig();
        loadPrinterConfig();
        setupActions();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void loadServerConfig() {
        binding.serverHostInput.setText(ConfigManager.getHost());
        binding.serverPortInput.setText(String.valueOf(ConfigManager.getPort()));
    }

    private void loadPrinterConfig() {
        String type = PrinterManager.getType();
        if (PrinterManager.TYPE_BLUETOOTH.equals(type)) {
            binding.bluetoothRadio.setChecked(true);
        } else if (PrinterManager.TYPE_USB.equals(type)) {
            binding.usbRadio.setChecked(true);
        } else {
            binding.networkRadio.setChecked(true);
        }
        binding.printerAddressInput.setText(PrinterManager.getAddress());
        binding.printerPortInput.setText(String.valueOf(PrinterManager.getPort()));
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
    }

    private void saveServer() {
        String host = text(binding.serverHostInput);
        int port = parseInt(binding.serverPortInput, 8080);
        ConfigManager.save(host, port);
        ApiClient.reset();
        binding.serverStatusText.setText("Saved. Server: " + ConfigManager.getBaseOrder());
    }

    private void savePrinter() {
        String type = currentPrinterType();
        String address = text(binding.printerAddressInput);
        int port = parseInt(binding.printerPortInput, 9100);
        PrinterManager.save(type, address, port);
        binding.printerStatusText.setText("Printer saved: " + type);
    }

    private String currentPrinterType() {
        int id = binding.printerTypeGroup.getCheckedRadioButtonId();
        if (id == R.id.bluetoothRadio) return PrinterManager.TYPE_BLUETOOTH;
        if (id == R.id.usbRadio) return PrinterManager.TYPE_USB;
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
            setPrinterStatus("No bonded Bluetooth devices found");
            return;
        }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = devices.get(i).getName() + "\n" + devices.get(i).getAddress();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Bluetooth Printer")
                .setItems(names, (d, which) -> {
                    String address = devices.get(which).getAddress();
                    binding.printerAddressInput.setText(address);
                    binding.bluetoothRadio.setChecked(true);
                    savePrinter();
                    setPrinterStatus("Bluetooth printer selected: " + address);
                })
                .show();
    }

    private void onUsbClick() {
        List<UsbDevice> devices = PrinterManager.getUsbPrinterDevices();
        if (devices.isEmpty()) {
            setPrinterStatus("No USB printer detected");
            return;
        }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = PrinterManager.usbDeviceDescription(devices.get(i));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select USB Printer")
                .setItems(names, (d, which) -> {
                    UsbDevice device = devices.get(which);
                    PrinterManager.saveUsbDevice(PrinterManager.usbDeviceDescription(device));
                    binding.usbRadio.setChecked(true);
                    binding.printerAddressInput.setText(PrinterManager.usbDeviceDescription(device));
                    savePrinter();
                    requestUsbThen(device, () ->
                            setPrinterStatus("USB printer granted"));
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
                () -> setPrinterStatus("USB permission denied"));
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
        setPrinterStatus("Printing...");
        PrinterManager.print(text, new PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                setPrinterStatus("Print sent successfully");
            }

            @Override
            public void onError(String message) {
                setPrinterStatus("Print failed: " + message);
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