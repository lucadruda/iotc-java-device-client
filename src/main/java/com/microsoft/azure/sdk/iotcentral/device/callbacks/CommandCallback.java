// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device.callbacks;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iotcentral.device.Command;

public class CommandCallback implements DeviceMethodCallback {
    private List<IoTCCallback> callbacks;

    public CommandCallback(List<IoTCCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public DeviceMethodData call(String methodName, Object methodData, Object context) {
        Command cmd = null;
        try {
            cmd = new Command(methodName, new String((byte[]) methodData, "UTF-8"), null);
        } catch (UnsupportedEncodingException e) {
            return new DeviceMethodData(500, "Payload with unsupported encoding");
        }
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).Exec(cmd);
        }
        return new DeviceMethodData(200, "Command " + methodName + " executed");
    }
}