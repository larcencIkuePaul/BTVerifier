package com.sanjetco.btverifier;

import java.util.UUID;

/**
 * Created by PaulLee on 11/7/2016.
 * Common definition
 */
public interface Common {
    /* Debug keyword */
    String DEBUG_KEYWORD = "BTVERIFIER";

    /* Amba. UUID definition */
    UUID AMBA_SERVICE_UUID = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");
    UUID AMBA_WRITE_UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
    UUID AMBA_READ_UUID = UUID.fromString("00003333-0000-1000-8000-00805f9b34fb");

    /* GATT status definition in bluedroid gatt_api.h */
    int GATT_ERROR = 0x85;

    /* Ambarella command definition */
    int AMBA_CMD_CREATE_SESSION = 257;
    int AMBA_CMD_DISCONN_SESSION = 258;

    /* Result definition */
    int AMBA_RESULT_FAILED = -1;
    int AMBA_RESULT_NO_RESP = -2;
    int AMBA_RESULT_RECV_HALF = -3;
    int AMBA_RESULT_GET_SERVICE_FAILED = -4;
    int AMBA_RESULT_SESSION_HOLDER = 1793;
    int AMBA_RESULT_CREATE_SESSION_OK = AMBA_CMD_CREATE_SESSION;
    int AMBA_RESULT_DISCONN_SESSION_OK = AMBA_CMD_DISCONN_SESSION;
}
