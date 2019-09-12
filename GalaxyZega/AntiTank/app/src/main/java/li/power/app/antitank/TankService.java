package li.power.app.antitank;

import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class TankService extends Service {

    private static final String TAG = "TankService";

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;
    private SampleAdvertiseCallback mAdvertiseCallback;

    private BluetoothGattCharacteristic tankReadCharacteristic;
    private BluetoothGattCharacteristic tankWriteCharacteristic;

    private StopAdvertisingReceiver mStopAdvertisingReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initialize();
        startAdvertising();
        mStopAdvertisingReceiver = new StopAdvertisingReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_STOP_ADV);
        registerReceiver(mStopAdvertisingReceiver, intentFilter);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private AdvertiseData buildAdvertiseData() {

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.TANK_SERVICE_UUID);
        dataBuilder.addManufacturerData(Constants.Manufacturer_ID, Constants.Manufacturer_Data);

        return dataBuilder.build();
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    private final BluetoothGattServerCallback mGattServerCallback =
            new BluetoothGattServerCallback() {

                @Override
                public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                    Log.d(TAG, "onConnectionStateChange: gatt server connection state changed, new state " + Integer.toString(newState));
                    super.onConnectionStateChange(device, status, newState);

                    switch(newState){
                        case BluetoothGattServer.STATE_CONNECTED:{
                            sendLogIntent("Connected");
                            break;
                        }
                        case BluetoothGattServer.STATE_DISCONNECTED:{
                            sendLogIntent("Disconnected");
                            break;
                        }
                    }
                }

                @Override
                public void onServiceAdded(int status, BluetoothGattService service) {
                    Log.d(TAG, "onServiceAdded: " + Integer.toString(status));
                    super.onServiceAdded(status, service);
                }

                @Override
                public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                    Log.d(TAG, "onCharacteristicReadRequest: " + "requestId" + Integer.toString(requestId) + "offset" + Integer.toString(offset));
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                }

                @Override
                public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

                    Log.d(TAG, "onCharacteristicWriteRequest: " + "data = " + bytesToHex(value));

                    sendLogIntent("<-- " + bytesToHex(value));

                    super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    byte[] ret = value;
                    for (String[] command : Constants.TANK_COMMANDS) {
                        if (command[0].equals(bytesToHex(value))) {
                            if(command[1].equals("")) return;
                            ret = hexToBytes(command[1]);
                        }
                    }
                    tankReadCharacteristic.setValue(ret);
                    mGattServer.notifyCharacteristicChanged(device, tankReadCharacteristic, false);
                    sendLogIntent((Arrays.equals(ret, value) ? "!!> " : "--> ") + bytesToHex(ret));
                }

                @Override
                public void onNotificationSent(BluetoothDevice device, int status) {
                    Log.d(TAG, "onNotificationSent: status = " + Integer.toString(status));
                    super.onNotificationSent(device, status);
                }

                @Override
                public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                    Log.d(TAG, "onDescriptorReadRequest: requestId = " + Integer.toString(requestId));
                    super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
                }

                @Override
                public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                    Log.d(TAG, "onDescriptorWriteRequest: requestId = " + Integer.toString(requestId));
                    super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }

                @Override
                public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                    Log.d(TAG, "onExecuteWrite: requestId = " + Integer.toString(requestId));
                    super.onExecuteWrite(device, requestId, execute);
                    /*in case we stored data before, just execute the write action*/
                }

                @Override
                public void onMtuChanged(BluetoothDevice device, int mtu) {
                    Log.d(TAG, "onMtuChanged: mtu = " + Integer.toString(mtu));
                }
            };

    private void addService() {

        mGattServer.clearServices();

        tankReadCharacteristic = new BluetoothGattCharacteristic(
                Constants.TANK_READ_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        tankWriteCharacteristic = new BluetoothGattCharacteristic(
                Constants.TANK_WRITE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        BluetoothGattDescriptor tankReadDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_WRITE);
        tankReadCharacteristic.addDescriptor(tankReadDescriptor);
        BluetoothGattService tankService = new BluetoothGattService(
                Constants.TANK_SERVICE_UUID.getUuid(),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        tankService.addCharacteristic(tankReadCharacteristic);
        tankService.addCharacteristic(tankWriteCharacteristic);

        mGattServer.addService(tankService);

    }

    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
                    if (mGattServer == null) {
                        Toast.makeText(this, "GATT Server null", Toast.LENGTH_LONG).show();
                    } else {
                        addService();
                    }
                }
            }
        }

    }

    private void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");

        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdvertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data,
                        mAdvertiseCallback);
            }
        }
    }

    private void sendLogIntent(String msg) {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_ADD_LOG);
        intent.putExtra(Constants.TAG_ADD_LOG, msg);
        sendBroadcast(intent);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hex) {
        return new UnsignedBigInteger(hex, 16).toUnsignedByteArray();
    }

    public class StopAdvertisingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Constants.ACTION_STOP_ADV)) {
                mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
                Log.d(TAG, "Stopped Advertising");
            }
            unregisterReceiver(mStopAdvertisingReceiver);
        }
    }

    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d(TAG, "Advertising failed " + errorCode);
            stopSelf();

        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }

    }
}
