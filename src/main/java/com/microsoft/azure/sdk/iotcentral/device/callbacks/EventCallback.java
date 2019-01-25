// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device.callbacks;

import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iotcentral.device.ILogger;

public class EventCallback implements IotHubEventCallback {

    private IoTCCallback callback;
    private ILogger logger;

    public EventCallback(IoTCCallback callback, ILogger logger) {
        this.callback = callback;
        this.logger = logger;
    }

    public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
        if (this.callback != null) {
            if (!responseStatus.equals(IotHubStatusCode.OK)) {
                this.callback.Exec(responseStatus.toString());
            } else {
                this.callback.Exec(responseStatus.toString());
            }
        } else {
            this.logger.Log(responseStatus.toString());
        }
    }
}