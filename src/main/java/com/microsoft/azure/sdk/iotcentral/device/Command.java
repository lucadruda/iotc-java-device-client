// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device;

import java.util.HashMap;
import java.util.Map;

public class Command {

    private String name;
    private String payload;
    private String requestId;

    public Command(String name, String payload, String requestId) {
        this.name = name;
        this.payload = payload;
        this.requestId = requestId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * @return the requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @param message The message to send back as a response for the command
     * @return Well-formed object to be sent as a property. The message will appear in the command tile in the application
     */
    public Object getResponseObject(String message) {
        Map<String, Object> respMap = new HashMap<String, Object>();
        HashMap<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("value", message);
        respMap.put(getName(), valueMap);
        return respMap;
    }

}