// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device;

import com.microsoft.azure.sdk.iotcentral.device.callbacks.IoTCCallback;
import com.microsoft.azure.sdk.iotcentral.device.enums.*;
import com.microsoft.azure.sdk.iotcentral.device.exceptions.IoTCentralException;

public interface IIoTCClient {
    /**
     * Set connection protocol (MQTT, AMQP, HTTPS). Must be called before Connect().
     * Default: MQTT
     */
    public void SetProtocol(IOTC_PROTOCOL transport);

    /**
     * Set DPS global endpoint. Must be called before Connect(). Default:
     * "global.azure-devices-provisioning.net"
     */
    public void SetGlobalEndpoint(String endpoint);

    /**
     * Set network proxy Must be called before Connect(). Default: no proxy
     */
    public void SetProxy(HTTP_PROXY_OPTIONS options);

    /**
     * Set logging level (FULL, API_ONLY, DISABLED). Can be changed at any time.
     * Default: DISABLED
     */
    public void SetLogging(IOTC_LOGGING logLevel);

    /**
     * Disconnect device. Callback is executed when device successfully disconnect.
     */
    public void Disconnect(IoTCCallback callback) throws IoTCentralException;

    /**
     * Connect device. Callback is executed when device successfully connect to
     * IoTCentral.
     */
    public void Connect() throws IoTCentralException;

    /**
     * Send a telemetry message. Can include multiple values in a flatten object
     */
    public void SendTelemetry(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Send a state message. Can include multiple values in a flatten object
     */
    public void SendState(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Send events. Can include multiple events in a flatten object
     */
    public void SendEvent(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Send update values to properties. Can include multiple properties in a
     * flatten object. If property is sent in the form
     * {propertyName:{value:"value"}} and propertyName is the name of a command,
     * then it sends updates to specific command tile in IoTCentral( e.g. command
     * progress)
     */
    public void SendProperty(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Listen to events.
     */
    public void on(IOTC_EVENTS event, IoTCCallback callback);

}