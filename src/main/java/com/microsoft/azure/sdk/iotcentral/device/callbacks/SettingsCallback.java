// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device.callbacks;

import java.util.List;

import com.microsoft.azure.sdk.iot.deps.twin.TwinCollection;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Property;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.TwinPropertyCallBack;
import com.microsoft.azure.sdk.iotcentral.device.Setting;

public class SettingsCallback implements TwinPropertyCallBack {
    private List<IoTCCallback> callbacks;

    public SettingsCallback(List<IoTCCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void TwinPropertyCallBack(Property property, Object context) {
        Setting setting = null;
        Object value = ((TwinCollection) property.getValue()).get("value");
        setting = new Setting(property.getKey(), value, property.getVersion());
        if (!property.getIsReported()) {
            for (int i = 0; i < this.callbacks.size(); i++) {
                this.callbacks.get(i).Exec(setting);
            }

        }
    }
}