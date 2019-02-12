// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device.callbacks;

import java.util.List;

import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.github.lucadruda.iotc.device.enums.IOTC_CONNECTION_STATE;

public class ConnectionStatus implements IotHubConnectionStatusChangeCallback {
    private List<IoTCCallback> callbacks;

    public ConnectionStatus(List<IoTCCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void execute(IotHubConnectionStatus status, IotHubConnectionStatusChangeReason statusChangeReason,
            Throwable throwable, Object callbackContext) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).Exec(IOTC_CONNECTION_STATE.valueOf(statusChangeReason.name()));
        }

    }
}