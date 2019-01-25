// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device;

import java.util.HashMap;
import java.util.Map;

public class Setting {

    private String name;
    private Object value;
    private int version;

    public Setting(String name, Object value, int version) {
        this.name = name;
        this.value = value;
        this.version = version;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param message The message to send back as a response for the command
     * @return Well-formed object to be sent as a property. The message will appear
     *         in the command tile in the application
     */
    public Object getResponseObject(String message) {
        Map<String, Object> respMap = new HashMap<String, Object>();
        HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("value", getValue());
        valueMap.put("message", message);
        valueMap.put("status", "completed");
        valueMap.put("desiredVersion", getVersion());
        respMap.put(getName(), valueMap);
        return respMap;
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

}