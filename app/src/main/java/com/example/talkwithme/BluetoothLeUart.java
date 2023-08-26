package com.example.talkwithme;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothLeUart extends BluetoothGattCallback{
    private String classname = getClass().getName();
    private Context context;
    private String deviceName;

    //UUIDs for UART service and associated characteristics.
    private static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static UUID TX_UUID   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static UUID RX_UUID   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    //UUID for the UART BTLE client characteristic which is necessary for notifications.
    private static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private WeakHashMap<Callback, Object> callbacks;
    BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private Queue<String> sendQueue;
    private boolean writeInProgress;

    private Queue<BluetoothGattCharacteristic> readQueue;

    //Interface for a BluetoothLeUart client to be notified of UART actions.
    public interface Callback {
        void onConnected(BluetoothLeUart uart);
        void onConnectFailed(BluetoothLeUart uart);
        void onDisconnected(BluetoothLeUart uart);
        void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx);
        void onDeviceFound(BluetoothDevice device);
    }

    BluetoothLeUart(Context context) {
        super();
        this.context = context;
        this.callbacks = new WeakHashMap<>();
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.gatt = null;
        this.tx = null;
        this.rx = null;
        this.sendQueue = new ConcurrentLinkedQueue<>();
        this.writeInProgress = false;
        this.readQueue = new ConcurrentLinkedQueue<>();
    }

    @SuppressWarnings("unused")
    public BluetoothGatt getGatt() {
        return gatt;
    }

    boolean isConnected() {
        return (tx != null && rx != null);
    }

    private void send() {
        if (tx == null || sendQueue.isEmpty() || gatt == null) {
            return;
        }
        String data = sendQueue.poll();
        if(data != null) {
            tx.setValue(data.getBytes(StandardCharsets.UTF_8));
            writeInProgress = true; // Set the write in progress flag
            gatt.writeCharacteristic(tx);
        }
    }

    void send(String data) {
        if(data.equals("")){
            return;
        }
        data = "ä" + data + "ü";
        //ble can send a maximum of 20 bytes each time
        while (data.length() > 20) {
            sendQueue.add(data.substring(0, 20));
            data = data.substring(20);
        }
        sendQueue.add(data);
        if (!writeInProgress) {
            send();
        }
    }

    //Register the specified callback to receive UART callbacks.
    void registerCallback(Callback callback) {
        callbacks.put(callback, null);
    }

    //Unregister the specified callback.
    void unregisterCallback(Callback callback) {
        callbacks.remove(callback);
    }

    //Disconnect to a device if currently connected.
    void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
        }
        gatt = null;
        tx = null;
        rx = null;
    }

    @RequiresApi(21)
    private void scanLeDevice21(boolean enable) {
        ScanCallback mLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (device.getName() != null) {
                    if (device.getName().equals(deviceName)) {
                        notifyOnDeviceFound(device);
                        GattClientCallback gattClientCallback = new GattClientCallback();
                        device.connectGatt(context, true, gattClientCallback);
                        adapter.getBluetoothLeScanner().stopScan(this);
                    }
                }
            }
        };
        BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            if(enable) {
                bluetoothLeScanner.startScan(mLeScanCallback);
            }else{
                bluetoothLeScanner.stopScan(mLeScanCallback);
            }
        }
    }

    private void scanLeDevice18(boolean enable) {
        BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (device.getName() != null){
                    if (device.getName().equals(deviceName)) {
                        notifyOnDeviceFound(device);
                        GattClientCallback gattClientCallback = new GattClientCallback();
                        device.connectGatt(context,true, gattClientCallback);
                        adapter.stopLeScan(this);
                    }
                }
            }
        };
        if(enable){
            adapter.startLeScan(mLeScanCallback);
        }else{
            adapter.stopLeScan(mLeScanCallback);
        }
    }

    //Stop any in progress UART device scan.
    @SuppressWarnings("unused")
    void stopScan() {
        if (adapter != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                scanLeDevice21(false);
            }else{
                scanLeDevice18(false);
            }
        }
    }

    void startScan(String deviceName) {
        if (deviceName.equals("")) {
            return;
        }
        this.deviceName = deviceName;
        if (adapter != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                scanLeDevice21(true);
            }else{
                scanLeDevice18(true);
            }
        }
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //Connected to device, start discovering services.
                    BluetoothLeUart.this.gatt = gatt;
                    if (!gatt.discoverServices()) {
                        //Error starting service discovery.
                        connectFailure();
                    }
                }else{
                    connectFailure();
                }
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                //Disconnected, notify callbacks of disconnection.
                rx = null;
                tx = null;
                notifyOnDisconnected(BluetoothLeUart.this);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //Notify connection failure if service discovery failed.
            if (status == BluetoothGatt.GATT_FAILURE) {
                connectFailure();
                return;
            }

            //Save reference to each UART characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

            //Setup notifications on RX characteristic changes (i.e. data received).
            //First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                //Stop if the characteristic notification setup failed.
                connectFailure();
                return;
            }
            //Next update the RX characteristic's client descriptor to enable notifications.
            BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
            if (desc == null) {
                //Stop if the RX characteristic has no client descriptor.
                connectFailure();
                return;
            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(desc)) {
                //Stop if the client descriptor could not be written.
                connectFailure();
                return;
            }
            //Notify of connection completion.
            notifyOnConnected(BluetoothLeUart.this);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            notifyOnReceive(BluetoothLeUart.this, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Check if there is anything left in the queue
                BluetoothGattCharacteristic nextRequest = readQueue.poll();
                if(nextRequest != null){
                    //Send a read request for the next item in the queue
                    gatt.readCharacteristic(nextRequest);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            writeInProgress = false;
            send();
        }
    }

    //Private functions to simplify the notification of all callbacks of a certain event.
    private void notifyOnConnected(BluetoothLeUart uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnected(uart);
            }
        }
    }

    private void notifyOnConnectFailed(BluetoothLeUart uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnectFailed(uart);
            }
        }
    }

    private void notifyOnDisconnected(BluetoothLeUart uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDisconnected(uart);
            }
        }
    }

    private void notifyOnReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null ) {
                cb.onReceive(uart, rx);
            }
        }
    }

    private void notifyOnDeviceFound(BluetoothDevice device) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceFound(device);
            }
        }
    }

    //Notify callbacks of connection failure, and reset connection state.
    private void connectFailure() {
        rx = null;
        tx = null;
        notifyOnConnectFailed(this);
    }
}