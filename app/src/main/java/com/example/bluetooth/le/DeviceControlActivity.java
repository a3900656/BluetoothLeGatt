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

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.MenuInflater;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private Button mEmailButton;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    public  BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean getdata = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private String Datastring="";
    //
    //private int groupID = 3,childID = 0;

    private int homepage_status = 0;
    private int setdata_status = 1;
    private int getdata_status =2;
    private int setrealtime_status =3;
    private int CSCV_status = 4;
    private int OptionManuStatus=0;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                isBusy = false;
                if (Datastring == ""){
                    Datastring = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                }else {
                    Datastring = Datastring + "\n" + intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
//                    Datastring = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                }
                displayData(Datastring);
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
//                            //mEmailButton.setText(Integer.toString(childPosition));
//                            if (characteristic.getUuid().toString().equals(SampleGattAttributes.UUID_GETDATA)) {
//                                if (mNotifyCharacteristic != null) {
//                                    mBluetoothLeService.setCharacteristicNotification(
//                                            mNotifyCharacteristic, false);
//                                    mNotifyCharacteristic = null;
//                                }
//                                mBluetoothLeService.writeCharacteristic(characteristic);
//                            }
//                            else if (characteristic.getUuid().toString().equals(SampleGattAttributes.UUID_REALTIME)){
//                                if (mNotifyCharacteristic != null) {
//                                    mBluetoothLeService.setCharacteristicNotification(
//                                            mNotifyCharacteristic, false);
//                                    mNotifyCharacteristic = null;
//                                }
//                                mBluetoothLeService.writeCharacteristic(characteristic);
//                            }
//                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // If there is an active notification on a characteristic, clear
//                            // it first so it doesn't update the data field on the user interface.
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            mBluetoothLeService.readCharacteristic(characteristic);
//                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            mNotifyCharacteristic = characteristic;
//                            mBluetoothLeService.setCharacteristicNotification(
//                                    characteristic, true);
//                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // Sets up UI references.
        //((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mEmailButton = (Button)findViewById(R.id.button);
        mEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OptionManuStatus = homepage_status;
                invalidateOptionsMenu();
                Datastring = "";
                displayData(Datastring);
//                testlist();
            }
        });
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
    public static boolean isBusy = false;
    private void testlist(){
        //get device info
        isBusy = true;
        mBluetoothLeService.getstatus(1);
        WriteReadBLE(type_getdata);
        while (isBusy) {

        }
        //get battery
        isBusy = true;
        mBluetoothLeService.getstatus(2);
        WriteReadBLE(type_getdata);
        while (isBusy) {
        }
        //get time
        isBusy = true;
        mBluetoothLeService.getstatus(9);
        WriteReadBLE(type_getdata);
        while (isBusy) {
        }
        //time sync
        isBusy = true;
        mBluetoothLeService.getstatus(2);
        WriteReadBLE(type_setdata);
        while (isBusy) {
        }
        //get EEPROM
        isBusy = true;
        mBluetoothLeService.getstatus(3);
        WriteReadBLE(type_getdata);
        WriteReadBLE(type_realtime);

    }

    private void WriteReadBLE(int type){
        if (mGattCharacteristics != null) {
            int gID=0,cID=0;
            if (type == type_getdata) {
                gID=4;cID=4;
            }
            else if (type == type_setdata){
                gID=4;cID=3;
            }
            else if (type == type_realtime){
                gID=4;cID=0;
            }
            else if (type == type_getCSCV){
                gID=5;cID=0;
            }
            final BluetoothGattCharacteristic characteristic =
                    mGattCharacteristics.get(gID).get(cID);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                if (characteristic.getUuid().toString().equals(SampleGattAttributes.UUID_SETDATA)){
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(
                                mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.writeCharacteristic(characteristic);
                }
                else if(characteristic.getUuid().toString().equals(SampleGattAttributes.UUID_GETDATA)){
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(
                                mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.writeCharacteristic(characteristic);
                }
            }
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                mNotifyCharacteristic = characteristic;
//                mBluetoothLeService.setCharacteristicNotification(
//                        characteristic, true);
//            }
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                mNotifyCharacteristic = characteristic;
//                mBluetoothLeService.setCharacteristicNotification(
//                        characteristic, true);
//            }
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                // If there is an active notification on a characteristic, clear
//                // it first so it doesn't update the data field on the user interface.
//                if (mNotifyCharacteristic != null) {
//                    mBluetoothLeService.setCharacteristicNotification(
//                            mNotifyCharacteristic, false);
//                    mNotifyCharacteristic = null;
//                }
//                mBluetoothLeService.readCharacteristic(characteristic);
//            }

        }
    }

    public void cleanOptionMenu(Menu menu){
        menu.findItem(R.id.menu_connect).setVisible(false);
        menu.findItem(R.id.Back).setVisible(false);
        menu.findItem(R.id.Getdata).setVisible(false);
        menu.findItem(R.id.GetDeviceInfo).setVisible(false);
        menu.findItem(R.id.GetBattery).setVisible(false);
        menu.findItem(R.id.GetEEPROM).setVisible(false);
        menu.findItem(R.id.GetDeviceTime).setVisible(false);
        menu.findItem(R.id.GetCSCV).setVisible(false);
        menu.findItem(R.id.CSCVON).setVisible(false);
        menu.findItem(R.id.CSCVOFF).setVisible(false);


        menu.findItem(R.id.Setdata).setVisible(false);
        menu.findItem(R.id.TimeSync).setVisible(false);
        menu.findItem(R.id.ClearData).setVisible(false);

        menu.findItem(R.id.Setrealtime).setVisible(false);
        menu.findItem(R.id.gyroX).setVisible(false);
        menu.findItem(R.id.gyroY).setVisible(false);
        menu.findItem(R.id.gyroZ).setVisible(false);
        menu.findItem(R.id.gyroXYZ).setVisible(false);
        menu.findItem(R.id.accX).setVisible(false);
        menu.findItem(R.id.accY).setVisible(false);
        menu.findItem(R.id.accZ).setVisible(false);
        menu.findItem(R.id.accXYZ).setVisible(false);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            if (OptionManuStatus == homepage_status) {
                cleanOptionMenu(menu);
                menu.findItem(R.id.Getdata).setVisible(true);
                menu.findItem(R.id.Setdata).setVisible(true);
                menu.findItem(R.id.Setrealtime).setVisible(true);
                menu.findItem(R.id.GetCSCV).setVisible(true);
            }
            else if (OptionManuStatus == getdata_status){
                cleanOptionMenu(menu);
                menu.findItem(R.id.Back).setVisible(true);
                menu.findItem(R.id.Getdata).setVisible(true);
                menu.findItem(R.id.GetDeviceInfo).setVisible(true);
                menu.findItem(R.id.GetBattery).setVisible(true);
                menu.findItem(R.id.GetEEPROM).setVisible(true);
                menu.findItem(R.id.GetDeviceTime).setVisible(true);//A9
            }
            else if (OptionManuStatus == setdata_status){
                cleanOptionMenu(menu);
                menu.findItem(R.id.Back).setVisible(true);
                menu.findItem(R.id.Setdata).setVisible(true);
                menu.findItem(R.id.TimeSync).setVisible(true);
                menu.findItem(R.id.ClearData).setVisible(true);
            }
            else if (OptionManuStatus == setrealtime_status){
                cleanOptionMenu(menu);
                menu.findItem(R.id.Back).setVisible(true);
                menu.findItem(R.id.Setrealtime).setVisible(true);
                menu.findItem(R.id.gyroX).setVisible(true);
                menu.findItem(R.id.gyroY).setVisible(true);
                menu.findItem(R.id.gyroZ).setVisible(true);
                menu.findItem(R.id.gyroXYZ).setVisible(true);
                menu.findItem(R.id.accX).setVisible(true);
                menu.findItem(R.id.accY).setVisible(true);
                menu.findItem(R.id.accZ).setVisible(true);
                menu.findItem(R.id.accXYZ).setVisible(true);
            }else if (OptionManuStatus == CSCV_status){
                cleanOptionMenu(menu);
                menu.findItem(R.id.Back).setVisible(true);
                menu.findItem(R.id.CSCVON).setVisible(true);
                menu.findItem(R.id.CSCVOFF).setVisible(true);
            }
        } else {
            cleanOptionMenu(menu);
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.Getdata).setVisible(false);
        }
        return true;
    }
    public static int type_getdata = 1;
    public static int type_setdata = 2;
    public static int type_realtime = 3;
    public static int type_getCSCV = 4;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.Back:
                OptionManuStatus = homepage_status;
                invalidateOptionsMenu();
                return true;
            case R.id.Getdata:
                OptionManuStatus = getdata_status;
                invalidateOptionsMenu();
                return true;
            case R.id.Setdata:
                OptionManuStatus = setdata_status;
                invalidateOptionsMenu();
                return true;
            case R.id.GetCSCV:
                OptionManuStatus = CSCV_status;
                invalidateOptionsMenu();
                return true;
            case R.id.TimeSync:
                mBluetoothLeService.getstatus(2);
                WriteReadBLE(type_setdata);
                return true;
            case R.id.ClearData:
                mBluetoothLeService.getstatus(3);
                WriteReadBLE(type_setdata);
                return true;
            case R.id.Setrealtime:
                OptionManuStatus = setrealtime_status;
                invalidateOptionsMenu();
                return true;
            case R.id.GetBattery:
                mBluetoothLeService.getstatus(2);
                WriteReadBLE(type_getdata);
                return true;
            case R.id.GetEEPROM:
                mBluetoothLeService.getstatus(3);
                isBusy = true;
                WriteReadBLE(type_realtime);
                SystemClock.sleep(1000);
                WriteReadBLE(type_getdata);
                return true;
            case R.id.GetDeviceTime:
                mBluetoothLeService.getstatus(9);
                WriteReadBLE(type_getdata);
                return true;
            case R.id.GetDeviceInfo:
                mBluetoothLeService.getstatus(1);
                WriteReadBLE(type_getdata);
                return true;
            case R.id.CSCVON:
                mBluetoothLeService.getstatus(1);
                WriteReadBLE(type_getCSCV);
                return true;
            case R.id.CSCVOFF:
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
