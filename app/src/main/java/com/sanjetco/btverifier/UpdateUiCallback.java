package com.sanjetco.btverifier;

import android.bluetooth.BluetoothDevice;

/**
 * Created by PaulLee on 11/7/2016.
 * Callback functions for MainUi
 */
public interface UpdateUiCallback {
    void clearScanResultCallback();
    void updateSearchDeviceCallback(BluetoothDevice device);
    void updateConnStateCallback(int state);
    void recvResponseFailedCallback();
    void getServiceFailedCallback();
    void updateAmbaSessionTokenNumberCallback(int token);
    void updateScanStateCallback(boolean isScanning);
    void updateServiceDiscoveryStateCallback(boolean isOK);
    void updateGattRetryCountCallback(int count);
    void updateIssueRetryCountCallback(int result);
    void updateBtVerifyResult(int result);
    void showLogCallback(String log);
}
