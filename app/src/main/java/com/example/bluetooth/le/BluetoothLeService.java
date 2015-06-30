/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT senrver hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_Battery_Service =
            UUID.fromString(SampleGattAttributes.UUID_Battery_Service);
    public final static UUID UUID_Device_Name =
            UUID.fromString(SampleGattAttributes.UUID_Device_Name);
    public final static UUID REALTIME_DATA_CHAR =
            UUID.fromString(SampleGattAttributes.REALTIME_DATA_CHAR);
    public final static UUID UUID_GETDATA =
            UUID.fromString(SampleGattAttributes.UUID_GETDATA);
    public final static UUID UUID_SETDATA =
            UUID.fromString(SampleGattAttributes.UUID_SETDATA);
    public final static UUID UUID_REALTIME =
            UUID.fromString(SampleGattAttributes.UUID_REALTIME);
    public final static UUID UUID_REALTIME_CONFIG =
            UUID.fromString(SampleGattAttributes.UUID_REALTIME_CONFIG);
    public final static UUID CSC_MEAS_UUID =
            UUID.fromString(SampleGattAttributes.CSC_MEAS_UUID);

    public int optionStatus=0;
    public int eepromPackageCounter = 0;
    public int prevCrankRevs = 0;
    public int prevCrankEvtTime = 0;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writebroadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
                mBluetoothGatt.readCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void getstatus(int status){
        optionStatus = status;
    }
    //////////////
    private void writebroadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        byte cmd[] = new byte[20];
        for (int i = 0; i < 20; i++) {
            if (i == 0) {
                cmd[i] = (byte) 0xA1;
            } else {
                cmd[i] = (byte) 0;
            }
        }
        characteristic.setValue(cmd);
    }
    ///////////////
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, "heart rate:"+String.valueOf(heartRate));
        } else if (UUID_Battery_Service.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int Batterylevel = characteristic.getIntValue(format, 0);
            Log.d(TAG, String.format("Received heart rate: %d", Batterylevel));
            intent.putExtra(EXTRA_DATA, String.valueOf(Batterylevel));
        }else if(UUID_Device_Name.equals(characteristic.getUuid())){
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final String Devicename = characteristic.getStringValue(0);
            Log.d(TAG, String.format("Received heart rate: %s", Devicename));
            intent.putExtra(EXTRA_DATA, Devicename);
        }
        else if(UUID_GETDATA.equals(characteristic.getUuid())) {

            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                //intent.putExtra(EXTRA_DATA, data[2] + "\n" + stringBuilder.toString());
                if (optionStatus == 1) {
                    intent.putExtra(EXTRA_DATA, "devType:" + ((data[2] * 255) + data[3]) + ", FW version:" + data[4] + "." + data[5] + "." + data[6]);
                } else if (optionStatus == 2) {
                    intent.putExtra(EXTRA_DATA, "Battery: " + data[2] + "\n" + "Battery Voltage: " + (data[3] + data[4] * 255));
                } else if (optionStatus == 9) {//device time
                    intent.putExtra(EXTRA_DATA, "s:" + data[2] + "m:" + data[3] + "h:" + data[4] + "d:" + data[5] + "w:" + data[6] + "m:" + data[7] + "y:" + (data[8] + 1970) + "Z:" + data[9]);
                } else {
                    intent.putExtra(EXTRA_DATA, String.format("%02X ",data[0]));
                }
            }
        }
        else if(UUID_REALTIME.equals(characteristic.getUuid())){
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data2 = characteristic.getValue();
            int data[] = new int[20];
            for(int i=0;i<20;i++){
                data[i] = (int)data2[i]&0xFF;
            }
            if (data2 != null && data2.length > 0) {
//                final StringBuilder stringBuilder = new StringBuilder(data.length);
//                for(byte byteChar : data)
//                    stringBuilder.append(String.format("%02X ", byteChar));
//                intent.putExtra(EXTRA_DATA, "here" + "\n" + stringBuilder.toString());
                int package_length= (data[7]+(data[6]<<8)+1);
                if (eepromPackageCounter == 0){
                    intent.putExtra(EXTRA_DATA,"Start High: "+data[2]+" Low: "+data[3]+" End High: "+data[4]+" Low: "+data[5]+"length: "
                            +package_length+"\n"+"TotalPackage Length: " + package_length);
                }
                else {
                    for (int i=0; i<5; i++) {
//                        int com = data[i * 4 + 0];
//                        int count1 = data[i * 4 + 1];
//                        int count2 = data[i * 4 + 2];
//                        int temp2 = data[i * 4 + 3];
                        if (data[i*4+0] == 5 || data[i*4+0] == 6 || data[i*4+0] == 19 ||data[i*4+0] == 20) {
                            intent.putExtra(EXTRA_DATA,"time: "+data[i*4+0]+"d: "+data[i*4+1]+"h: "+data[i*4+2]+"m: "+data[i*4+3]);
                        }
                        else if(data[i*4+0] == 17){
                            intent.putExtra(EXTRA_DATA,"time: "+data[i*4+0]+"d: "+data[i*4+1]+"h: "+data[i*4+2]+"m: "+data[i*4+3]);
                        }
                        else if(data[i*4+0] == 255){
                            intent.putExtra(EXTRA_DATA,"discard: "+data[i*4+0]+", "+data[i*4+1]+", "+data[i*4+2]+", "+data[i*4+3]);
                        }
                        else if(data[i*4+0] == 18){
                            intent.putExtra(EXTRA_DATA,"sleep: "+data[i*4+0]+", "+data[i*4+1]+", "+data[i*4+2]+", "+data[i*4+3]);
                        }
                        else{
                            intent.putExtra(EXTRA_DATA,"sport: "+data[i*4+0]+",: "+data[i*4+1]+",: "+data[i*4+2]+",: "+data[i*4+3]);
                        }
                    }
                }
                eepromPackageCounter++;
                if (eepromPackageCounter == package_length){

                }
            }
            else {
                //intent.putExtra(EXTRA_DATA,"EMPTY");
            }
        }
        else if(CSC_MEAS_UUID.equals(characteristic.getUuid())){
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                int currentCrankRevs = data[7]+data[8]*255;
                int currentCrankEvtTime = data[9]+data[10]*255;
//                intent.putExtra(EXTRA_DATA,"currentCrankRevs: "+currentCrankRevs+" currentCrankEvtTime: "+currentCrankEvtTime );
                int _pulse =currentCrankRevs - prevCrankRevs;
                int _eventTime  =currentCrankEvtTime-prevCrankEvtTime;
                float intervalOfEvent =((float)_eventTime/1024)/(float)_pulse;
                float _currentRPM=0;
                if(intervalOfEvent>0){
                    _currentRPM = 60/(float)intervalOfEvent;

                }
                intent.putExtra(EXTRA_DATA,"RPM: "+Float.toString(_currentRPM));
                prevCrankRevs = currentCrankRevs;
                prevCrankEvtTime = currentCrankEvtTime;
            }
        }
        DeviceControlActivity.isBusy = false;
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        else if (UUID_REALTIME.equals(characteristic.getUuid())){

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            //Log.w(TAG, "---------------------------------------");
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
        else if (CSC_MEAS_UUID.equals(characteristic.getUuid())){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        //DeviceControlActivity.isBusy = false;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        byte cmd[] = new byte[20];
        if (UUID_GETDATA.equals(characteristic.getUuid())) {
            if (optionStatus == 1) {//DeviceInfo
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xA0;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
            } else if (optionStatus == 2) {//Battery
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xA2;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
            }else if (optionStatus == 3) {//GetEEPROM
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xA1;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
                eepromPackageCounter = 0;
            }else if (optionStatus == 9) {//DeviceTime
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xA9;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
            } else {
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xA3;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
            }
        }
        else if (UUID_SETDATA.equals(characteristic.getUuid())) {
                    Calendar C = Calendar.getInstance();
            if (optionStatus == 1){//software reset
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xF1;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
            }
            else if (optionStatus == 2) {//TimeSync
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xF2;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
                cmd[1] = (byte) (C.get(Calendar.YEAR) - 1970);
                cmd[2] = (byte) (C.get(Calendar.MONTH) + 1);
                cmd[3] = (byte) (C.get(Calendar.DAY_OF_WEEK) - 1);
                cmd[4] = (byte) C.get(Calendar.DAY_OF_MONTH);
                cmd[5] = (byte) C.get(Calendar.HOUR);
                cmd[6] = (byte) C.get(Calendar.MINUTE);
                cmd[7] = (byte) C.get(Calendar.SECOND);
                cmd[8] = (byte) (C.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000));
            }
            else if(optionStatus == 3) {//ClearData
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xF3;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }

            }
            else if(optionStatus == 4) {//SetAlarms
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xF4;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }

            }
        }
        else if (UUID_REALTIME.equals(characteristic.getUuid())){
            if (optionStatus == 3){//GetEEPROM
                Log.w(TAG, "---------------------------------------");
                for (int i = 0; i < 20; i++) {
                    if (i == 0) {
                        cmd[i] = (byte) 0xA1;
                    } else {
                        cmd[i] = (byte) 0;
                    }
                }
            }
        }
        characteristic.setValue(cmd);
        mBluetoothGatt.writeCharacteristic(characteristic);
        DeviceControlActivity.isBusy = false;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
