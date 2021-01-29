// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device.models;

import com.github.lucadruda.iotc.device.enums.IOTC_COMMAND_RESPONSE;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;

public class IoTCCommand {

    private String name;
    private String componentName;
    private Object requestPayload;
    private CommandResponse response;

    public IoTCCommand(String componentName, String name, Object requestPayload, CommandResponse response) {
        this.name = name;
        this.requestPayload = requestPayload;
        this.response = response;
        this.componentName = componentName;
    }

    public IoTCCommand(String componentName, String name, Object requestPayload) {
        this(componentName, name, requestPayload, null);
    }

    public DeviceMethodData reply(IOTC_COMMAND_RESPONSE status, String message) {
        if (this.response == null) {
            return null;
        }
        String name = this.componentName != null ? String.format("%s*%s", this.componentName, this.name) : this.name;
        this.response.sendResponse(name, message);
        return new DeviceMethodData(this.getStatus(status), message);
    }

    public Object getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(Object requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getName() {
        return name;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IoTCCommand(String name) {
        this(name, null, null);
    }

    private int getStatus(IOTC_COMMAND_RESPONSE status) {
        switch (status) {
            default:
                return 200;
            case ERROR:
                return 500;
        }
    }

}