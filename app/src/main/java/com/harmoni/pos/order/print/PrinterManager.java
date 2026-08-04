package com.harmoni.pos.order.print;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterManager {

    public interface PrintCallback {
        void onSuccess();
        void onError(String message);
    }

    public static final String TYPE_NETWORK = "network";
    public static final String TYPE_BLUETOOTH = "bluetooth";
    public static final String TYPE_USB = "usb";

    private static final String PREFS = "printer_prefs";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_USB_DEVICE = "usb_device";

    private static final String ACTION_USB_PERMISSION = "com.harmoni.pos.order.USB_PERMISSION";
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int DEFAULT_NETWORK_PORT = 9100;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Context context;
    private static SharedPreferences prefs;

    private PrinterManager() {}

    public static void init(Context appContext) {
        if (context == null) {
            context = appContext.getApplicationContext();
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    public static String getType() {
        return prefs != null ? prefs.getString(KEY_TYPE, TYPE_NETWORK) : TYPE_NETWORK;
    }

    public static String getAddress() {
        return prefs != null ? prefs.getString(KEY_ADDRESS, "") : "";
    }

    public static int getPort() {
        return prefs != null ? prefs.getInt(KEY_PORT, DEFAULT_NETWORK_PORT) : DEFAULT_NETWORK_PORT;
    }

    public static void save(String type, String address, int port) {
        if (prefs == null) return;
        prefs.edit()
                .putString(KEY_TYPE, type)
                .putString(KEY_ADDRESS, address)
                .putInt(KEY_PORT, port)
                .apply();
    }

    // ------------------------------------------------------------------
    // Printing
    // ------------------------------------------------------------------

    public static void print(String text, PrintCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                byte[] data = buildBytes(text);
                send(data);
                MAIN.post(() -> {
                    if (callback != null) callback.onSuccess();
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    private static void send(byte[] data) throws Exception {
        switch (getType()) {
            case TYPE_BLUETOOTH:
                sendBluetooth(data);
                break;
            case TYPE_USB:
                sendUsb(data);
                break;
            case TYPE_NETWORK:
            default:
                sendNetwork(data);
                break;
        }
    }

    private static void sendNetwork(byte[] data) throws Exception {
        String address = getAddress();
        if (address == null || address.trim().isEmpty()) {
            throw new Exception("Network printer address not set");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address.trim(), getPort()), 5000);
            OutputStream out = socket.getOutputStream();
            out.write(data);
            out.flush();
        }
    }

    private static void sendBluetooth(byte[] data) throws Exception {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            throw new Exception("Bluetooth permission not granted");
        }
        String mac = getAddress();
        if (mac == null || mac.trim().isEmpty()) {
            throw new Exception("Bluetooth printer not selected");
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new Exception("Bluetooth not supported");
        }
        BluetoothDevice device = adapter.getRemoteDevice(mac.trim());
        try (BluetoothSocketHolder holder = openSocket(device)) {
            holder.socket.getOutputStream().write(data);
            holder.socket.getOutputStream().flush();
        }
    }

    private static class BluetoothSocketHolder implements AutoCloseable {
        final android.bluetooth.BluetoothSocket socket;

        BluetoothSocketHolder(android.bluetooth.BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static BluetoothSocketHolder openSocket(BluetoothDevice device) throws Exception {
        android.bluetooth.BluetoothSocket socket =
                device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
        socket.connect();
        return new BluetoothSocketHolder(socket);
    }

    private static void sendUsb(byte[] data) throws Exception {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new Exception("USB not supported");
        }
        UsbDevice device = getConfiguredUsbDevice();
        if (device == null) {
            throw new Exception("USB printer not found");
        }
        if (!usbManager.hasPermission(device)) {
            throw new Exception("USB permission not granted");
        }
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            throw new Exception("Cannot open USB device");
        }
        try {
            UsbInterface printerInterface = findPrinterInterface(device);
            if (printerInterface == null || !connection.claimInterface(printerInterface, true)) {
                throw new Exception("Cannot claim USB printer interface");
            }
            UsbEndpoint outEndpoint = findBulkOut(printerInterface);
            if (outEndpoint == null) {
                throw new Exception("No USB output endpoint found");
            }
            int written = 0;
            while (written < data.length) {
                int n = connection.bulkTransfer(outEndpoint, data, written,
                        data.length - written, 10000);
                if (n < 0) {
                    throw new Exception("USB write failed");
                }
                written += n;
            }
            connection.releaseInterface(printerInterface);
        } finally {
            connection.close();
        }
    }

    // ------------------------------------------------------------------
    // USB helpers
    // ------------------------------------------------------------------

    public static List<UsbDevice> getUsbPrinterDevices() {
        List<UsbDevice> result = new ArrayList<>();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return result;
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        for (UsbDevice device : devices.values()) {
            if (findPrinterInterface(device) != null) {
                result.add(device);
            }
        }
        return result;
    }

    public static String usbDeviceDescription(UsbDevice device) {
        return device.getDeviceName();
    }

    public static boolean hasUsbPermission(UsbDevice device) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager != null && usbManager.hasPermission(device);
    }

    public static void requestUsbPermission(Activity activity, UsbDevice device,
                                            Runnable onGranted, Runnable onDenied) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null || device == null) {
            if (onDenied != null) onDenied.run();
            return;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                activity.unregisterReceiver(this);
                if (granted && onGranted != null) onGranted.run();
                else if (onDenied != null) onDenied.run();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        activity.registerReceiver(receiver, filter);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        usbManager.requestPermission(device, pendingIntent);
    }

    public static void saveUsbDevice(String deviceName) {
        if (prefs == null) return;
        prefs.edit().putString(KEY_USB_DEVICE, deviceName).apply();
    }

    private static UsbDevice getConfiguredUsbDevice() {
        String deviceName = prefs != null ? prefs.getString(KEY_USB_DEVICE, "") : "";
        if (deviceName.isEmpty()) return null;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return null;
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        return devices.get(deviceName);
    }

    private static UsbInterface findPrinterInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            if (iface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                return iface;
            }
        }
        return null;
    }

    private static UsbEndpoint findBulkOut(UsbInterface iface) {
        for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = iface.getEndpoint(i);
            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                return endpoint;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Bluetooth helpers
    // ------------------------------------------------------------------

    public static List<BluetoothDevice> getBondedBluetoothDevices() {
        List<BluetoothDevice> result = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return result;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return result;
        Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        if (bonded != null) {
            result.addAll(bonded);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // ESC/POS bytes
    // ------------------------------------------------------------------

    public static byte[] buildBytes(String text) {
        byte[] init = new byte[]{0x1B, 0x40};
        byte[] textBytes = text == null ? new byte[0] : text.getBytes();
        byte[] feed = new byte[]{0x1B, 0x64, 0x04};
        byte[] cut = new byte[]{0x1D, 0x56, 0x42};
        byte[] result = new byte[init.length + textBytes.length + feed.length + cut.length];
        System.arraycopy(init, 0, result, 0, init.length);
        System.arraycopy(textBytes, 0, result, init.length, textBytes.length);
        int offset = init.length + textBytes.length;
        System.arraycopy(feed, 0, result, offset, feed.length);
        System.arraycopy(cut, 0, result, offset + feed.length, cut.length);
        return result;
    }
}
