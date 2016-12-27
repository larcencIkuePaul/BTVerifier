package com.sanjetco.btverifier;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Created by PaulLee on 11/7/2016.
 * GATT manager
 */
public class GattManager implements Common {

    Context mContext;
    MainUi mMainUi;
    BluetoothGatt mGatt;
    BluetoothGattCharacteristic mAmbaReadChars, mAmbaWriteChars;
    int mTokenNumber = 0;
    int mGattRetryCount = 0;
    BluetoothDevice mCurrentDevice;
    String mRecvMessage = null;
    boolean mIsMessageVerifierThreadRunning = false;
    boolean mIsAmbaSessHolderCountdownThreadRunning = false;
    boolean mIsWaitAmbaCmdFeedbackThreadRunning = false;

    static final int MAX_ATTEMPT_GATT_CONN_RETRY_COUNT = 10;
    static final int SLEEP_INTERVAL_MILLISEC = 1000;
    static final int MAX_WAIT_RESPONSE_MESSAGE_RETRY_COUNT = 10;
    static final int MAX_WAIT_AMBA_SESS_HOLDER_RETRY_COUNT = 10;
    static final int MAX_WAIT_AMBA_CMD_FEEDBACK_RETRY_COUNT = 10;

    GattManager (Context context) {
        mContext = context;
        mMainUi = (MainUi) context;
    }

    protected void connToDevice(BluetoothDevice device) {
        mCurrentDevice = device;
        String name = mCurrentDevice.getName();
        String address = mCurrentDevice.getAddress();
        Log.d(DEBUG_KEYWORD, "Selected device info");
        Log.d(DEBUG_KEYWORD, "Device name: " + name);
        Log.d(DEBUG_KEYWORD, "Device address: " + address);
        mMainUi.showLog("<font color='grey'>Selected device info</font>");
        mMainUi.showLog("<font color='grey'>Device name: " + name + "</font>");
        mMainUi.showLog("<font color='grey'>Device address: " + address + "</font>");
        getBondState();
        mCurrentDevice.connectGatt(mContext, false, gattCallback);
    }

    protected void connToDevice() {
        if (mCurrentDevice != null) {
            String name = mCurrentDevice.getName();
            String address = mCurrentDevice.getAddress();
            Log.d(DEBUG_KEYWORD, "Selected device info");
            Log.d(DEBUG_KEYWORD, "Device name: " + name);
            Log.d(DEBUG_KEYWORD, "Device address: " + address);
            mMainUi.showLog("<font color='grey'>Selected device info</font>");
            mMainUi.showLog("<font color='grey'>Device name: " + name + "</font>");
            mMainUi.showLog("<font color='grey'>Device address: " + address + "</font>");
            getBondState();
            mCurrentDevice.connectGatt(mContext, false, gattCallback);
        } else {
            Log.d(DEBUG_KEYWORD, "No device selected");
            mMainUi.showLog("No device selected");
        }
    }

    protected void getBondState() {
        switch (mCurrentDevice.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                Log.d(DEBUG_KEYWORD, "Remote device is BONDED");
                mMainUi.showLog("<font color='grey'>Remote device is BONDED</font>");
                break;
            case BluetoothDevice.BOND_BONDING:
                Log.d(DEBUG_KEYWORD, "Remote device is BONDING");
                mMainUi.showLog("<font color='grey'>Remote device is BONDING</font>");
                break;
            case BluetoothDevice.BOND_NONE:
                Log.d(DEBUG_KEYWORD, "Remote device is no BOND");
                mMainUi.showLog("<font color='grey'>Remote device is no BOND</font>");
                break;
        }
    }

    protected void disconnGattFromDevice() {
        if (mGatt != null) {
            mGatt.disconnect();
            Log.d(DEBUG_KEYWORD, "GATT is DISCONNECTING");
            mMainUi.showLog("GATT is DISCONNECTING");
        }
    }

    protected void closeGattFromDevice() {
        if (mGatt != null) {
            mGatt.close();
            Log.d(DEBUG_KEYWORD, "GATT is CLOSING");
            mMainUi.showLog("GATT is CLOSING");
        }
    }

    protected void discoveryServices() {
        if (mGatt != null) {
            if (mGatt.discoverServices()) {
                Log.d(DEBUG_KEYWORD, "GATT service discovery is STARTED");
                mMainUi.showLog("GATT service discovery is <font color='green'>STARTED</font>");
            }
        }
    }

    protected void createAmbaSession() {
        if (mGatt != null) {
            if (mAmbaWriteChars != null) {
                mMainUi.showLog("AMBA session is <font color='green'>CREATING</font>");
                sendMessageToAmbaDevice(createAmbaFormatMessage(AMBA_CMD_CREATE_SESSION));
            }
        }
    }

    protected void disconnAmbaSession() {
        if (mGatt != null) {
            if (mAmbaWriteChars != null) {
                if (mTokenNumber > 0) {
                    mMainUi.showLog("AMBA session is <font color='red'>DISCONNECTING</font>");
                    sendMessageToAmbaDevice(createAmbaFormatMessage(AMBA_CMD_DISCONN_SESSION));
                }
            }
        }
    }

    protected void sendMessageToAmbaDevice(String msg) {
        mAmbaWriteChars.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mAmbaWriteChars.setValue(msg);
        mGatt.writeCharacteristic(mAmbaWriteChars);
    }

    protected String createAmbaFormatMessage(int msgId) {
        JSONObject jsonObject = new JSONObject();
        switch (msgId) {
            case AMBA_CMD_CREATE_SESSION:
                try {
                    jsonObject.put("token", mTokenNumber);
                    jsonObject.put("msg_id", msgId);
                } catch (JSONException e) {
                    Log.d(DEBUG_KEYWORD, e.getMessage());
                }
                break;
            case AMBA_CMD_DISCONN_SESSION:
                try {
                    jsonObject.put("token", mTokenNumber);
                    jsonObject.put("msg_id", msgId);
                } catch (JSONException e) {
                    Log.d(DEBUG_KEYWORD, e.getMessage());
                }
                break;
        }
        String msg = "Command: " + jsonObject.toString();
        Log.d(DEBUG_KEYWORD, msg);
        mMainUi.showLog(msg);
        return jsonObject.toString();
    }

    protected void readAmbaResponse() {
        if (mGatt != null) {
            if (mAmbaReadChars != null)
                mGatt.readCharacteristic(mAmbaReadChars);
        }
    }

    protected String getReceivedMessage() {
        if (mRecvMessage != null)
            return mRecvMessage;
        else
            return "Received message is null";
    }

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(DEBUG_KEYWORD, "status: " + status);
            Log.d(DEBUG_KEYWORD, "newState: " + newState);
            mMainUi.showLog("status: " + status);
            mMainUi.showLog("newState: " + newState);
            mMainUi.updateConnStateCallback(newState);
            //if (status == 133 || status == 22) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (mGattRetryCount >= MAX_ATTEMPT_GATT_CONN_RETRY_COUNT) {
                    mGattRetryCount = 0;
                    resetTokenNumber();
                    closeGattFromDevice();
                    mMainUi.updateAmbaSessionTokenNumberCallback(mTokenNumber);
                    mMainUi.updateGattRetryCountCallback(mGattRetryCount);
                    mMainUi.updateIssueRetryCountCallback(AMBA_RESULT_FAILED);
                    mMainUi.updateBtVerifyResult(AMBA_RESULT_FAILED);
                    Log.d(DEBUG_KEYWORD, "Achieve max GATT retry counts");
                    mMainUi.showLog("<font color='red'>Achieve max Gatt retry counts</font>");
                } else {
                    mGattRetryCount++;
                    mMainUi.updateGattRetryCountCallback(mGattRetryCount);
                    try {
                        Thread.sleep(SLEEP_INTERVAL_MILLISEC);
                    } catch (InterruptedException e) {
                        Log.d(DEBUG_KEYWORD, e.getMessage());
                    }
                    connToDevice();
                }
            } else {
                mGattRetryCount = 0;
                mGatt = gatt;
                mMainUi.updateGattRetryCountCallback(mGattRetryCount);
                startServiceDiscovery();
            }
        }

        protected void startServiceDiscovery() {
            try {
                Thread.sleep(SLEEP_INTERVAL_MILLISEC);
            } catch (InterruptedException e) {
                Log.d(DEBUG_KEYWORD, e.getMessage());
            }
            discoveryServices();
        }

        protected void startCreateAmbaSession() {
            try {
                Thread.sleep(SLEEP_INTERVAL_MILLISEC);
            } catch (InterruptedException e) {
                Log.d(DEBUG_KEYWORD, e.getMessage());
            }
            createAmbaSession();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> serviceList = gatt.getServices();
                for (BluetoothGattService service : serviceList) {
                    if (AMBA_SERVICE_UUID.equals(service.getUuid())) {
                        Log.d(DEBUG_KEYWORD, "AMBA Service UUID: " + service.getUuid().toString());
                        findReadAndWriteCharacteristics(service.getCharacteristics());
                    } else {
                        Log.d(DEBUG_KEYWORD, "Skip UUID: " + service.getUuid().toString());
                    }
                }
                if (mAmbaReadChars != null && mAmbaWriteChars != null) {
                    mMainUi.showLog("<font color='green'>Get GATT services OK #2</font>");
                    mMainUi.updateServiceDiscoveryStateCallback(true);
                    startCreateAmbaSession();
                } else {
                    mMainUi.showLog("<font color='red'>Get GATT services FAILED</font>");
                    mMainUi.updateServiceDiscoveryStateCallback(false);
                    mMainUi.getServiceFailedCallback();
                    mMainUi.updateBtVerifyResult(AMBA_RESULT_GET_SERVICE_FAILED);
                }
            } else {
                mMainUi.showLog("<font color='red'>Get GATT services FAILED</font>");
                mMainUi.updateServiceDiscoveryStateCallback(false);
                mMainUi.getServiceFailedCallback();
                mMainUi.updateBtVerifyResult(AMBA_RESULT_GET_SERVICE_FAILED);
            }
        }

        protected void findReadAndWriteCharacteristics(List<BluetoothGattCharacteristic> list) {
            for (BluetoothGattCharacteristic characteristic : list) {
                if (AMBA_READ_UUID.equals(characteristic.getUuid())) {
                    mAmbaReadChars = characteristic;
                    Log.d(DEBUG_KEYWORD, "AMBA read UUID: " + characteristic.getUuid().toString());
                    mGatt.setCharacteristicNotification(mAmbaReadChars, true);
                } else if (AMBA_WRITE_UUID.equals(characteristic.getUuid())) {
                    mAmbaWriteChars = characteristic;
                    Log.d(DEBUG_KEYWORD, "AMBA write UUID: " + characteristic.getUuid().toString());
                } else {
                    Log.d(DEBUG_KEYWORD, "Skip characteristic UUID: " + characteristic.getUuid().toString());
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(DEBUG_KEYWORD, "UUID: " + characteristic.getUuid().toString() + " read");
            mIsAmbaSessHolderCountdownThreadRunning = false;
            mIsWaitAmbaCmdFeedbackThreadRunning = false;
            if (BluetoothGatt.GATT_SUCCESS == status) {
                String message = characteristic.getStringValue(0);
                Log.d(DEBUG_KEYWORD, message);
                mMainUi.showLog(message);
                parseReceivedMessage(message);
            }
        }

        protected void parseReceivedMessage(String msg) {
            boolean bHasLeftBracket = msg.startsWith("{");
            boolean bHasRightBracket = msg.endsWith("}");
            if (bHasLeftBracket && bHasRightBracket) {
                Log.d(DEBUG_KEYWORD, "Receive completed message from remote device");
                mRecvMessage = msg;
                actionOfMsgid(parseMsgId());
            } else if (bHasLeftBracket) {
                if (mRecvMessage == null) {
                    Log.d(DEBUG_KEYWORD, "Receive message only has left bracket");
                    mMainUi.showLog("<font color='grey'>Receive message only has left bracket</font>");
                    //mIsAmbaSessHolderCountdownThreadRunning = false;
                    mRecvMessage = msg;
                    if (!mIsMessageVerifierThreadRunning) {
                        mIsMessageVerifierThreadRunning = true;
                        recvMessageVerifierThread();
                    }
                }
            } else if (bHasRightBracket) {
                if (mRecvMessage.startsWith("{")) {
                    if (!mRecvMessage.endsWith("}")) {
                        Log.d(DEBUG_KEYWORD, "Receive message only has right bracket");
                        mMainUi.showLog("<font color='grey'>Receive message only has right bracket</font>");
                        mRecvMessage = mRecvMessage.concat(msg);
                        if (!mIsMessageVerifierThreadRunning) {
                            if ((isValidJsonMessage())) {
                                Log.d(DEBUG_KEYWORD, "Receive completed message: " + mRecvMessage);
                                mMainUi.showLog("<font color='grey'>Receive completed message:</font>");
                                mMainUi.showLog("<font color='grey'>" + mRecvMessage + "</font>");
                                actionOfMsgid(parseMsgId());
                            }
                        }
                    }
                }
            }
        }

        protected void recvMessageVerifierThread() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int count = 0;
                    while (count < MAX_WAIT_RESPONSE_MESSAGE_RETRY_COUNT) {
                        try {
                            Thread.sleep(SLEEP_INTERVAL_MILLISEC);
                        } catch (InterruptedException e) {
                            Log.d(DEBUG_KEYWORD, e.getMessage());
                        }
                        if (isValidJsonMessage())
                            break;
                        count++;
                    }
                    if (count < MAX_WAIT_RESPONSE_MESSAGE_RETRY_COUNT) {
                        Log.d(DEBUG_KEYWORD, "Receive completed message: " + mRecvMessage);
                        mMainUi.showLog("<font color='grey'>Receive completed message:</font>");
                        mMainUi.showLog("<font color='grey'>" + mRecvMessage + "</font>");
                        actionOfMsgid(parseMsgId());
                    } else {
                        Log.d(DEBUG_KEYWORD, "Failed to receive completed message");
                        mMainUi.showLog("<font color='red'>Failed to receive completed message</font>");
                        resetRecvMessage();
                        resetTokenNumber();
                        mMainUi.recvResponseFailedCallback();
                        mMainUi.updateAmbaSessionTokenNumberCallback(mTokenNumber);
                        mMainUi.updateBtVerifyResult(AMBA_RESULT_RECV_HALF);
                    }
                    mIsMessageVerifierThreadRunning = false;
                }
            }).start();
        }

        protected boolean isValidJsonMessage() {
            if (mRecvMessage != null) {
                try {
                    new JSONObject(mRecvMessage);
                } catch (JSONException e) {
                    return false;
                }
                return true;
            } else {
                Log.d(DEBUG_KEYWORD, "Recv msg is null");
                mMainUi.showLog("<font color='red'>Recv msg is null</font>");
                return false;
            }
        }

        protected int parseMsgId() {
            int msgId = -1;
            try {
                JSONObject object = new JSONObject(mRecvMessage);
                msgId = object.getInt("msg_id");
            } catch (JSONException e) {
                Log.d(DEBUG_KEYWORD, e.getMessage());
            }
            return msgId;
        }

        protected void actionOfMsgid(int msgId) {
            Log.d(DEBUG_KEYWORD, "msg_id: " + msgId);
            switch (msgId) {
                case AMBA_RESULT_CREATE_SESSION_OK:
                    getAmbaSessionToken();
                    resetRecvMessage();
                    mMainUi.updateBtVerifyResult(msgId);
                    break;
                case AMBA_RESULT_DISCONN_SESSION_OK:
                    resetTokenNumber();
                    resetRecvMessage();
                    mMainUi.updateAmbaSessionTokenNumberCallback(mTokenNumber);
                    mMainUi.updateBtVerifyResult(msgId);
                    break;
                case AMBA_RESULT_SESSION_HOLDER:
                    resetRecvMessage();
                    if (!mIsAmbaSessHolderCountdownThreadRunning) {
                        mIsAmbaSessHolderCountdownThreadRunning = true;
                        ambaSessHolderCountdownThread();
                    }
                    break;
                case AMBA_RESULT_FAILED:
                    Log.d(DEBUG_KEYWORD, "Parse Amba. msg_id failed");
                    mMainUi.showLog("<font color='red'>Parse Amba. msg_id failed</font>");
                    break;
                default:
                    Log.d(DEBUG_KEYWORD, "Parse Amba. msg_id failed");
                    mMainUi.showLog("<font color='red'>Parse Amba. msg_id failed</font>");
                    break;
            }
        }

        protected void getAmbaSessionToken() {
            try {
                JSONObject object = new JSONObject(mRecvMessage);
                mTokenNumber = object.getInt("param");
                Log.d(DEBUG_KEYWORD, String.format(Locale.getDefault(), "Get token(%d) OK", mTokenNumber));
                mMainUi.showLog(String.format(Locale.getDefault(), "Get token(%d) <font color='green'>OK</font>", mTokenNumber));
                mMainUi.updateAmbaSessionTokenNumberCallback(mTokenNumber);
            } catch (JSONException e) {
                Log.d(DEBUG_KEYWORD, e.getMessage());
            }
        }

        protected void ambaSessHolderCountdownThread() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(DEBUG_KEYWORD, "AMBA session holder thread START");
                    int count = 0;
                    while (count < MAX_WAIT_AMBA_SESS_HOLDER_RETRY_COUNT && mIsAmbaSessHolderCountdownThreadRunning) {
                        try {
                            Thread.sleep(SLEEP_INTERVAL_MILLISEC);
                        } catch (InterruptedException e) {
                            Log.d(DEBUG_KEYWORD, e.getMessage());
                        }
                        count++;
                    }
                    if (count >= MAX_WAIT_AMBA_SESS_HOLDER_RETRY_COUNT) {
                        Log.d(DEBUG_KEYWORD, "AMBA session holder timeout");
                        resetRecvMessage();
                        resetTokenNumber();
                        mMainUi.recvResponseFailedCallback();
                        mMainUi.updateAmbaSessionTokenNumberCallback(mTokenNumber);
                        mMainUi.updateBtVerifyResult(AMBA_RESULT_SESSION_HOLDER);
                        mIsAmbaSessHolderCountdownThreadRunning = false;
                    }
                    mIsAmbaSessHolderCountdownThreadRunning = false;
                    Log.d(DEBUG_KEYWORD, "AMBA session holder thread END");
                }
            }).start();
        }

        protected void resetTokenNumber() {
            mTokenNumber = 0;
        }

        protected void resetRecvMessage() {
            mRecvMessage = null;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(DEBUG_KEYWORD, "Write callback: " + characteristic.getStringValue(0));
            mMainUi.showLog("<font color='grey'>Write callback: " + characteristic.getStringValue(0) + "</font>");
            if (BluetoothGatt.GATT_SUCCESS == status) {
                Log.d(DEBUG_KEYWORD, "Write GATT OK");
                mMainUi.showLog("<font color='grey'>Write GATT OK</font>");
                if (!mIsWaitAmbaCmdFeedbackThreadRunning) {
                    mIsWaitAmbaCmdFeedbackThreadRunning = true;
                    waitAmbaCmdFeedbackThread();
                }
            }
        }

        protected void waitAmbaCmdFeedbackThread() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(DEBUG_KEYWORD, "Wait AMBA command feedback thread START");
                    int count = 0;
                    while (count < MAX_WAIT_AMBA_CMD_FEEDBACK_RETRY_COUNT && mIsWaitAmbaCmdFeedbackThreadRunning) {
                        try {
                            Thread.sleep(SLEEP_INTERVAL_MILLISEC);
                        } catch (InterruptedException e) {
                            Log.d(DEBUG_KEYWORD, e.getMessage());
                            mMainUi.showLog("<font color='red'>" + e.getMessage() + "</font>");
                        }
                        count++;
                    }
                    if (count >= MAX_WAIT_AMBA_CMD_FEEDBACK_RETRY_COUNT) {
                        Log.d(DEBUG_KEYWORD, "Wait AMBA command feedback timeout");
                        resetRecvMessage();
                        resetTokenNumber();
                        mMainUi.recvResponseFailedCallback();
                        mMainUi.updateAmbaSessionTokenNumberCallback(mTokenNumber);
                        mMainUi.updateBtVerifyResult(AMBA_RESULT_NO_RESP);
                        mIsWaitAmbaCmdFeedbackThreadRunning = false;
                    }
                    mIsWaitAmbaCmdFeedbackThreadRunning = false;
                    Log.d(DEBUG_KEYWORD, "Wait AMBA command feedback thread END");
                }
            }).start();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(DEBUG_KEYWORD, "UUID: " + characteristic.getUuid().toString() + " changed");
            mIsAmbaSessHolderCountdownThreadRunning = false;
            mIsWaitAmbaCmdFeedbackThreadRunning = false;
            String message = characteristic.getStringValue(0);
            Log.d(DEBUG_KEYWORD, message);
            mMainUi.showLog("<font color='grey'>" + message + "</font>");
            parseReceivedMessage(message);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }
    };
}
