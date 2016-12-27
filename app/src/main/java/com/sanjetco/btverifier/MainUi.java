package com.sanjetco.btverifier;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainUi extends AppCompatActivity implements Common, UpdateUiCallback {

    Context mContext;
    BtAdministrator mBtAdministrator;
    GattManager mGattManager;
    ListView mListView;
    ArrayAdapter<String> mListViewAdapter;
    ArrayList<BluetoothDevice> mDeviceList;
    BluetoothDevice mCurrentDevice;
    int mCurrentGattConnState;
    int mTokenNumber = 0;
    int mIssueRetryCount = 0;

    static final int CHECK_CONN_STATE_AFTER_GATT_CLOSE_MILLISEC = 2000;
    static final int ISSUE_RETRY_INTERVAL_MILLISEC = 2000;
    static final int SCAN_STATE_FINISH = 0;
    static final int SCAN_STATE_SCANNING = 1;
    static final int SERVICE_DISCOVERY_STATE_OK = 0;
    static final int SERVICE_DISCOVERY_STATE_NONE = 1;
    static final int MAX_ISSUE_RETRY_COUNT = 10;
    static final int PROGRESS_BAR_DO_NOTHING = 0;
    static final int PROGRESS_BAR_INVISIBLE = 1;
    static final int PROGRESS_BAR_VISIBLE = 2;

    /* UI elements */
    Switch mSwitchConnState;
    Switch mSwitchScanState;
    Switch mSwitchServiceDiscoveryState;
    TextView mTextConnState;
    TextView mTextScanState;
    TextView mTextServiceDiscoveryState;
    TextView mTextAmbaSessionTokenNumber;
    TextView mTextConnRetryCount;
    TextView mTextIssueRetryCount;
    LinearLayout mLayoutProgressBar;
    ScrollView mViewShowLog;
    TextView mTextShowLog;
    LinearLayout mLayoutDeviceListProgressBar;

    Handler mConnStateToastHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            CharSequence info;
            switch (msg.what) {
                case BluetoothProfile.STATE_CONNECTED:
                    info = "Device is connected";
                    positiveConnState();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    info = "Device is disconnected";
                    negativeConnAndServiceDiscoveryState();
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    info = "Device is connecting";
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    info = "Device is disconnecting";
                    break;
                default:
                    info = "Unrecognized connection state";
                    break;
            }
            /*
            LayoutInflater inflater = MainUi.this.getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast, (ViewGroup) MainUi.this.findViewById(R.id.layoutToastCommon));
            TextView text = (TextView) layout.findViewById(R.id.textToastCommon);
            text.setText(info);
            Toast toast = new Toast(MainUi.this);
            toast.setView(layout);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
            */
            //Toast.makeText(mContext, info, Toast.LENGTH_SHORT).show();
        }
    };

    Handler mScanStateHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCAN_STATE_FINISH:
                    mSwitchScanState.setChecked(false);
                    mTextScanState.setText(R.string.text_state_scan_finish);
                    mTextScanState.setTextColor(Color.GRAY);
                    mLayoutProgressBar.setVisibility(View.INVISIBLE);
                    break;
                case SCAN_STATE_SCANNING:
                    mSwitchScanState.setChecked(true);
                    mTextScanState.setText(R.string.text_state_scanning);
                    mTextScanState.setTextColor(Color.CYAN);
                    mLayoutProgressBar.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    Handler mServiceDiscoveryStateHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_DISCOVERY_STATE_OK:
                    mSwitchServiceDiscoveryState.setChecked(true);
                    mTextServiceDiscoveryState.setText(R.string.text_state_service_ok);
                    mTextServiceDiscoveryState.setTextColor(Color.GREEN);
                    break;
                case SERVICE_DISCOVERY_STATE_NONE:
                    mSwitchServiceDiscoveryState.setChecked(false);
                    mTextServiceDiscoveryState.setText(R.string.text_state_service_none);
                    mTextServiceDiscoveryState.setTextColor(Color.RED);
                    break;
            }
        }
    };

    Handler mAmbaSessStateToastHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String logMsg, toastMsg;
            if (msg.what > 0) {
                mTokenNumber = msg.what;
                logMsg = "<font color='green'>Establish AMBA Session OK #3</font>";
                toastMsg = "Establish AMBA Session OK";
                mTextAmbaSessionTokenNumber.setText(String.valueOf(msg.what));
                mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
            } else {
                if (mTokenNumber > 0) {
                    logMsg = "<font color='green'>Close AMBA Session OK #4</font>";
                    toastMsg = "Close AMBA Session OK";
                } else {
                    logMsg = "<font color='red'>Establish AMBA Session FAILED #3</font>";
                    toastMsg = "Establish AMBA Session FAILED";
                    //mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
                }
                mTextAmbaSessionTokenNumber.setText(String.valueOf(msg.what));
            }
            showLog(logMsg);
            LayoutInflater inflater = MainUi.this.getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast, (ViewGroup) MainUi.this.findViewById(R.id.layoutToastCommon));
            TextView text = (TextView) layout.findViewById(R.id.textToastCommon);
            text.setText(toastMsg);
            Toast toast = new Toast(MainUi.this);
            toast.setView(layout);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
            //Toast.makeText(mContext, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    Handler mGattRetryCountHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            mTextConnRetryCount.setText(String.valueOf(msg.what));
        }
    };

    Handler mIssueRetryCountHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AMBA_RESULT_FAILED:
                    /*
                    mIssueRetryCount = 0;
                    mTextIssueRetryCount.setText(String.valueOf(0));
                    */
                    break;
                case AMBA_RESULT_NO_RESP:
                case AMBA_RESULT_RECV_HALF:
                case AMBA_RESULT_SESSION_HOLDER:
                case AMBA_RESULT_GET_SERVICE_FAILED:
                    mTextIssueRetryCount.setText(String.valueOf(mIssueRetryCount));
                    LayoutInflater inflater = MainUi.this.getLayoutInflater();
                    View layout = inflater.inflate(R.layout.toast, (ViewGroup) MainUi.this.findViewById(R.id.layoutToastCommon));
                    TextView text = (TextView) layout.findViewById(R.id.textToastCommon);
                    text.setText(String.format(Locale.getDefault(), "Issue, retry %d time", mIssueRetryCount));
                    Toast toast = new Toast(MainUi.this);
                    toast.setView(layout);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
                    //Toast.makeText(MainUi.this, String.format(Locale.getDefault(), "Issue, retry %d time", mIssueRetryCount), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    Handler mDeviceListProgressBarHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROGRESS_BAR_INVISIBLE:
                    mLayoutDeviceListProgressBar.setVisibility(View.INVISIBLE);
                    break;
                case PROGRESS_BAR_VISIBLE:
                    mLayoutDeviceListProgressBar.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    protected void positiveConnState() {
        mSwitchConnState.setChecked(true);
        mTextConnState.setText(R.string.text_state_conn);
        mTextConnState.setTextColor(Color.GREEN);
    }

    protected void negativeConnAndServiceDiscoveryState() {
        mSwitchConnState.setChecked(false);
        mTextConnState.setText(R.string.text_state_disconn);
        mTextConnState.setTextColor(Color.RED);
        mSwitchServiceDiscoveryState.setChecked(false);
        mTextServiceDiscoveryState.setText(R.string.text_state_service_none);
        mTextServiceDiscoveryState.setTextColor(Color.RED);
    }

    protected void negativeServiceDiscoveryState() {
        mSwitchServiceDiscoveryState.setChecked(false);
        mTextServiceDiscoveryState.setText(R.string.text_state_service_none);
        mTextServiceDiscoveryState.setTextColor(Color.RED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();
        Log.d(DEBUG_KEYWORD, "App created");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add(int gropuId, int itemId, int order, int resourceId)
        menu.add(0, 0, 0, R.string.btn_scan);
        /*
        menu.add(0, 1, 1, R.string.btn_read_characteristic);
        menu.add(0, 2, 2, R.string.btn_stop_scan);
        */
        //menu.add(0, 3, 3, R.string.btn_stop_amba_session);
        /*
        menu.add(0, 4, 4, R.string.btn_close_gatt_connection);
        menu.add(0, 5, 5, R.string.btn_get_connection_state);
        menu.add(0, 6, 6, R.string.btn_discovery_service);
        menu.add(0, 7, 7, R.string.btn_create_amba_session);
        menu.add(0, 8, 8, R.string.btn_read_amba_response);
        menu.add(0, 9, 9, R.string.btn_disconnect_amba_session);
        menu.add(0, 10, 10, R.string.btn_open_pushcam);
        */
        //menu.add(0, 11, 11, R.string.btn_check_thread_state);
        /*
        menu.add(0, 12, 12, R.string.btn_check_received_message);
        menu.add(0, 99, 99, R.string.btn_close_app);
        */
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                mBtAdministrator.startScan();
                break;
            case 2:
                mBtAdministrator.stopScan();
                break;
            case 4:
                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED ||
                        mCurrentGattConnState == BluetoothProfile.STATE_CONNECTING) {
                    disconnAndCloseGatt(PROGRESS_BAR_INVISIBLE);
                    mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
                }
                break;
            case 5:
                if (mCurrentDevice != null)
                    mCurrentGattConnState = mBtAdministrator.getConnState(mCurrentDevice);
                break;
            case 6:
                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(DEBUG_KEYWORD, "Start services discovery");
                    showLog("Start services discovery");
                    mGattManager.discoveryServices();
                }
                break;
            case 7:
                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED)
                    mGattManager.createAmbaSession();
                break;
            case 8:
                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED)
                    mGattManager.readAmbaResponse();
                break;
            case 9:
                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED)
                    mGattManager.disconnAmbaSession();
                break;
            case 10:
                Log.d(DEBUG_KEYWORD, "Load Pushcam...");
                Intent intent = new Intent(this, ViewWeb.class);
                startActivity(intent);
                break;
            case 12:
                Log.d(DEBUG_KEYWORD, mGattManager.getReceivedMessage());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED ||
                mCurrentGattConnState == BluetoothProfile.STATE_CONNECTING) {
            disconnAndCloseGatt(PROGRESS_BAR_INVISIBLE);
            mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
        }
        mBtAdministrator.unregBtReceiver();
        Log.d(DEBUG_KEYWORD, "App destroyed");
    }

    @Override
    public void clearScanResultCallback() {
        clear();
    }

    @Override
    public void updateSearchDeviceCallback(BluetoothDevice device) {
        if (!mDeviceList.contains(device)) {
            mDeviceList.add(device);
            final String newDevice = device.getName() + " " + device.getAddress();
            mListViewAdapter.add(newDevice);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListView.smoothScrollToPosition(mListViewAdapter.getPosition(newDevice));
                }
            });
        }
    }

    @Override
    public void updateConnStateCallback(int state) {
        mCurrentGattConnState = state;
        if (state == BluetoothProfile.STATE_CONNECTED)
            showLog("<font color='green'>GATT is CONNECTED #1</font>");
        mBtAdministrator.getConnState(mCurrentDevice); // Force to print connection state
        mConnStateToastHandler.sendEmptyMessage(state);
    }

    @Override
    public void recvResponseFailedCallback() {
        if (mCurrentDevice != null)
            mCurrentGattConnState = mBtAdministrator.getConnState(mCurrentDevice);

        switch (mCurrentGattConnState) {
            case BluetoothProfile.STATE_CONNECTED:
                disconnAndCloseGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                closeGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            case BluetoothProfile.STATE_CONNECTING:
                disconnAndCloseGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                closeGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            default:
                Log.d(DEBUG_KEYWORD, "Unrecognized connection state");
                showLog("<font color='red'>Unrecognized connection state</font>");
                break;
        }
    }

    @Override
    public void getServiceFailedCallback() {
        if (mCurrentDevice != null)
            mCurrentGattConnState = mBtAdministrator.getConnState(mCurrentDevice);

        switch (mCurrentGattConnState) {
            case BluetoothProfile.STATE_CONNECTED:
                disconnAndCloseGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                closeGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            case BluetoothProfile.STATE_CONNECTING:
                disconnAndCloseGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                closeGatt(PROGRESS_BAR_DO_NOTHING);
                break;
            default:
                Log.d(DEBUG_KEYWORD, "Unrecognized connection state");
                showLog("<font color='red'>Unrecognized connection state</font>");
                break;
        }
    }

    protected void disconnAndCloseGatt(int actDevListProgBar) {
        mGattManager.disconnGattFromDevice();
        mGattManager.closeGattFromDevice();
        checkConnStateAfterClose(actDevListProgBar);
    }

    protected void closeGatt(int actDevListProgBar) {
        mGattManager.closeGattFromDevice();
        checkConnStateAfterClose(actDevListProgBar);
    }

    protected void checkConnStateAfterClose(final int actDevListProgBar) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(CHECK_CONN_STATE_AFTER_GATT_CLOSE_MILLISEC);
                } catch (InterruptedException e) {
                    Log.d(DEBUG_KEYWORD, e.getMessage());
                }
                mCurrentGattConnState = mBtAdministrator.getConnState(mCurrentDevice);
                if (mCurrentGattConnState == BluetoothProfile.STATE_DISCONNECTED)
                    showLog("<font color='green'>Close GATT OK #5</font>");
                mConnStateToastHandler.sendEmptyMessage(mCurrentGattConnState);
                if (actDevListProgBar == PROGRESS_BAR_INVISIBLE) {
                    mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
                    showResult("DISCONN VERIFY OK", Color.GREEN, AMBA_RESULT_DISCONN_SESSION_OK);
                }
            }
        }).start();
    }

    @Override
    public void updateAmbaSessionTokenNumberCallback(int token) {
        mAmbaSessStateToastHandler.sendEmptyMessage(token);
    }

    @Override
    public void updateScanStateCallback(boolean isScanning) {
        if (isScanning) {
            mScanStateHandler.sendEmptyMessage(SCAN_STATE_SCANNING);
            mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
        }
        else
            mScanStateHandler.sendEmptyMessage(SCAN_STATE_FINISH);
    }

    @Override
    public void updateServiceDiscoveryStateCallback(boolean isOK) {
        if (isOK)
            mServiceDiscoveryStateHandler.sendEmptyMessage(SERVICE_DISCOVERY_STATE_OK);
        else
            mServiceDiscoveryStateHandler.sendEmptyMessage(SERVICE_DISCOVERY_STATE_NONE);
    }

    @Override
    public void updateGattRetryCountCallback(int count) {
        mGattRetryCountHandler.sendEmptyMessage(count);
    }

    @Override
    public void updateIssueRetryCountCallback(int result) {
        switch (result) {
            case AMBA_RESULT_FAILED:
                mIssueRetryCountHandler.sendEmptyMessage(result);
                break;
        }
    }

    @Override
    public void updateBtVerifyResult(int result) {
        switch (result) {
            case AMBA_RESULT_CREATE_SESSION_OK:
                showResult("CONN VERIFY OK", Color.GREEN, result);
                break;
            case AMBA_RESULT_DISCONN_SESSION_OK:
                //showResult("AMBA SESSION DISCONNECTED OK", Color.GREEN);
                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED ||
                        mCurrentGattConnState == BluetoothProfile.STATE_CONNECTING) {
                    disconnAndCloseGatt(PROGRESS_BAR_INVISIBLE);
                    //mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
                }
                break;
            case AMBA_RESULT_FAILED:
                mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
                showResult("VERIFY FAILED", Color.RED, result);
                break;
            case AMBA_RESULT_NO_RESP:
            case AMBA_RESULT_RECV_HALF:
            case AMBA_RESULT_SESSION_HOLDER:
            case AMBA_RESULT_GET_SERVICE_FAILED:
                if (mIssueRetryCount < MAX_ISSUE_RETRY_COUNT) {
                    mIssueRetryCount++;
                    mIssueRetryCountHandler.sendEmptyMessage(result);
                    try {
                        Thread.sleep(ISSUE_RETRY_INTERVAL_MILLISEC);
                    } catch (InterruptedException e) {
                        Log.d(DEBUG_KEYWORD, e.getMessage());
                        showLog("<font color='red'>" + e.getMessage() + "</font>");
                    }
                    mGattManager.connToDevice(mCurrentDevice);
                } else {
                    mIssueRetryCountHandler.sendEmptyMessage(AMBA_RESULT_FAILED);
                    mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_INVISIBLE);
                    showResult("VERIFY FAILED", Color.RED, result);
                }
                break;
        }
    }

    @Override
    public void showLogCallback(String log) {
        showLog(log);
    }

    private void showResult(final String msg, final int colorId, final int result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView view = new TextView(MainUi.this);
                view.setTextColor(colorId);
                view.setText(msg);
                view.setTextSize(15);
                view.setPadding(20, 50, 20, 20);
                view.setGravity(Gravity.CENTER);
                switch (colorId) {
                    case Color.RED:
                        new AlertDialog.Builder(MainUi.this)
                                .setTitle(R.string.dialog_title)
                                .setPositiveButton(R.string.dialog_ok, null)
                                .setCancelable(false)
                                .setView(view)
                                .show();
                        break;
                    case Color.GREEN:
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainUi.this)
                                .setTitle(R.string.dialog_title)
                                .setCancelable(false)
                                .setView(view);
                        switch (result) {
                            case AMBA_RESULT_CREATE_SESSION_OK:
                                dialog.setPositiveButton(R.string.dialog_disconn, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mCurrentGattConnState == BluetoothProfile.STATE_CONNECTED) {
                                                    mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_VISIBLE);
                                                    mGattManager.disconnAmbaSession();
                                                }
                                            }
                                        }).start();
                                    }
                                });
                                break;
                            case AMBA_RESULT_DISCONN_SESSION_OK:
                                dialog.setPositiveButton(R.string.dialog_ok, null);
                                break;
                        }
                        dialog.show();
                        break;
                }
            }
        });
    }

    AdapterView.OnItemClickListener scanResultClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int itemIndex, long itemId) {
            if (mBtAdministrator.isScanning()) {
                mBtAdministrator.stopScan();
                LayoutInflater inflater = MainUi.this.getLayoutInflater();
                View layout = inflater.inflate(R.layout.toast, (ViewGroup) MainUi.this.findViewById(R.id.layoutToastCommon));
                TextView text = (TextView) layout.findViewById(R.id.textToastCommon);
                text.setText(R.string.toast_stop_scanning_and_select_device_again);
                Toast toast = new Toast(MainUi.this);
                toast.setView(layout);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.show();
                //Toast toast = Toast.makeText(mContext, R.string.toast_stop_scanning_and_select_device_again, Toast.LENGTH_SHORT);
            } else {
                negativeServiceDiscoveryState();
                mCurrentDevice = mDeviceList.get(itemIndex);
                mDeviceListProgressBarHandler.sendEmptyMessage(PROGRESS_BAR_VISIBLE);
                mIssueRetryCount = 0;
                mTextIssueRetryCount.setText(String.valueOf(0));
                mGattManager.connToDevice(mCurrentDevice);
            }
        }
    };

    protected void showLog(final String log) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextShowLog.append(Html.fromHtml(log));
                //mTextShowLog.append(Html.fromHtml("<br>"));
                mTextShowLog.append("\n");
                mViewShowLog.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    protected void initialization() {
        mContext = getApplicationContext();
        mBtAdministrator = new BtAdministrator(this);
        mBtAdministrator.initBtAdapter();
        mBtAdministrator.regBtReceiver();
        mGattManager = new GattManager(this);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setOnItemClickListener(scanResultClickListener);
        mDeviceList = new ArrayList<>();
        mListViewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        mListView.setAdapter(mListViewAdapter);
        mCurrentGattConnState = BluetoothProfile.STATE_DISCONNECTED;
        initUiElements();
    }

    protected void initUiElements() {
        mSwitchConnState = (Switch) findViewById(R.id.switchConnState);
        mSwitchScanState = (Switch) findViewById(R.id.switchScanState);
        mSwitchServiceDiscoveryState = (Switch) findViewById(R.id.switchServiceDiscoveryState);
        mTextConnState = (TextView) findViewById(R.id.textConnState);
        mTextConnState.setTextColor(Color.RED);
        mTextScanState = (TextView) findViewById(R.id.textScanState);
        mTextScanState.setTextColor(Color.GRAY);
        mTextServiceDiscoveryState = (TextView) findViewById(R.id.textServiceDiscoveryState);
        mTextServiceDiscoveryState.setTextColor(Color.RED);
        mTextAmbaSessionTokenNumber = (TextView) findViewById(R.id.textAmbaSessTokenNumberValue);
        mTextConnRetryCount = (TextView) findViewById(R.id.textConnRetryCountValue);
        mTextIssueRetryCount = (TextView) findViewById(R.id.textIssueRetryCountValue);
        mLayoutProgressBar = (LinearLayout) findViewById(R.id.layoutProgressBar);
        mLayoutProgressBar.setVisibility(View.INVISIBLE);
        mViewShowLog = (ScrollView) findViewById(R.id.viewShowLog);
        mTextShowLog = (TextView) findViewById(R.id.textShowLog);
        mLayoutDeviceListProgressBar = (LinearLayout) findViewById(R.id.layoutDeviceListProgressBar);
        mLayoutDeviceListProgressBar.setVisibility(View.INVISIBLE);
        mLayoutDeviceListProgressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    protected void clear() {
        mDeviceList.clear();
        mListViewAdapter.clear();
        mTextShowLog.setText("");
    }
}
