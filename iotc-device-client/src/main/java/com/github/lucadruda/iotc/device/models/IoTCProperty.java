// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device.models;

public class IoTCProperty {

    private String name;
    private String componentName;
    private Object value;
    private int version;
    private PropertyResponse response;

    public IoTCProperty(String componentName, String name, Object value, int version, PropertyResponse response) {
        this.name = name;
        this.value = value;
        this.version = version;
        this.response = response;
        this.componentName = componentName;
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
     */
    public void ack(String message) {
        String strVal = value.toString();
        if (value instanceof String) {
            strVal = String.format("\"%s\"", value);
        }
        if (this.componentName != null) {
            this.response.sendResponse(
                    String.format("{\"%s\":{\"__t\":\"c\",\"%s\":{\"ac\":%d,\"ad\":\"%s\",\"value\":%s,\"av\":%d}}}",
                            getComponentName(), getName(), 200, message.isEmpty() ? "Property applied" : message,
                            strVal, getVersion()));
        } else {
            this.response.sendResponse(String.format("{\"%s\":{\"ac\":%d,\"ad\":\"%s\",\"value\":%s,\"av\":%d}}",
                    getName(), 200, message.isEmpty() ? "Property applied" : message, strVal, getVersion()));
        }
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * 
     * @return component name
     */
    public String getComponentName() {
        return componentName;
    }

}