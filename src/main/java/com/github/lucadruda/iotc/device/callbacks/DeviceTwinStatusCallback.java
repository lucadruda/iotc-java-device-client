// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device.callbacks;

import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;

public class DeviceTwinStatusCallback implements IotHubEventCallback {

    private IoTCCallback callback;

    public DeviceTwinStatusCallback(IoTCCallback callback) {
        this.callback = callback;
    }

    @Override
    public void execute(IotHubStatusCode status, Object callbackContext) {
        if (this.callback != null) {
            if ((status == IotHubStatusCode.OK) || (status == IotHubStatusCode.OK_EMPTY)) {
                this.callback.Exec(status.toString());
            } else {
                this.callback.Exec(status.toString());
            }
        }
    }

}