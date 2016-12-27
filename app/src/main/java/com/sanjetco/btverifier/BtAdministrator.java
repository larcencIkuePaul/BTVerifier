package com.sanjetco.btverifier;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Locale;

/**
 * Created by PaulLee on 11/7/2016.
 * Bluetooth manager
 */
public class BtAdministrator implements Common {

    Context mContext;
    MainUi mMainUi;
    BluetoothAdapter mBtAdapter;
    BluetoothManager mBluetoothManager;

    BtAdministrator(Context context) {
        mContext = context;
        mMainUi = (MainUi) context;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    protected void initBtAdapter() {
        if (mBtAdapter == null) {
            //mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            mBtAdapter = mBluetoothManager.getAdapter();
        }
        if (!mBtAdapter.isEnabled())
            mBtAdapter.enable();
    }

    protected void regBtReceiver() {
        IntentFilter filter;
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(btDeviceReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(btAdapterReceiver, filter);
    }

    protected void unregBtReceiver() {
        mContext.unregisterReceiver(btDeviceReceiver);
        mContext.unregisterReceiver(btAdapterReceiver);
    }

    protected void startScan() {
        if (mBtAdapter != null)
            if (!mBtAdapter.isDiscovering()) {
                mMainUi.clearScanResultCallback();
                mBtAdapter.startDiscovery();
            }
    }

    protected boolean stopScan() {
        boolean result = true;
        if (mBtAdapter != null) {
            if (mBtAdapter.isDiscovering())
                result = mBtAdapter.cancelDiscovery();
        }
        return result;
    }

    protected boolean isScanning() {
        return mBtAdapter != null && mBtAdapter.isDiscovering();
    }

    protected int getConnState(BluetoothDevice device) {
        int state = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        String message = "Connection state:";
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                message = String.format("%s <font color='red'>DISCONNECTED</font>", message);
                break;
            case BluetoothProfile.STATE_CONNECTING:
                message = String.format("%s <font color='green'>CONNECTING</font>", message);
                break;
            case BluetoothProfile.STATE_CONNECTED:
                message = String.format("%s <font color='green'>CONNECTED</font>", message);
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                message = String.format("%s <font color='red'>DISCONNECTING</font>", message);
                break;
        }
        Log.d(DEBUG_KEYWORD, message);
        mMainUi.showLog(message);
        return state;
    }

    BroadcastReceiver btDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String msg = String.format(Locale.getDefault(),
                        "Name: %s MAC: %s Bond state: %d Type: %d", device.getName(), device.getAddress(), device.getBondState(), device.getType());
                Log.d(DEBUG_KEYWORD, msg);
                mMainUi.updateSearchDeviceCallback(device);
            }
        }
    };

    BroadcastReceiver btAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                Log.d(DEBUG_KEYWORD, "Scanning is FINISHED");
                mMainUi.showLog("Scanning is <font color='green'>FINISHED</font>");
                mMainUi.updateScanStateCallback(false);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                Log.d(DEBUG_KEYWORD, "Scanning is STARTED");
                mMainUi.showLog("Scanning is <font color='green'>STARTED</font>");
                mMainUi.updateScanStateCallback(true);
            }
        }
    };
}
