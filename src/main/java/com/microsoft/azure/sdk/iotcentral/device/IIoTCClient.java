// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device;

import com.microsoft.azure.sdk.iotcentral.device.callbacks.IoTCCallback;
import com.microsoft.azure.sdk.iotcentral.device.enums.*;
import com.microsoft.azure.sdk.iotcentral.device.exceptions.IoTCentralException;

public interface IIoTCClient {
    /**
     * Set connection protocol (MQTT, AMQP, HTTPS). Must be called before Connect().
     * 
     * @param transport Trasport to set. Default: MQTT
     * 
     */
    public void SetProtocol(IOTC_PROTOCOL transport);

    /**
     * Set DPS global endpoint. Must be called before Connect()
     * 
     * @param endpoint Endpoint of DPS server. Default:
     *                 "global.azure-devices-provisioning.net"
     * 
     */
    public void SetGlobalEndpoint(String endpoint);

    /**
     * Set network proxy Must be called before Connect(). Default: no proxy
     * 
     * @param options Proxy options
     * 
     */
    public void SetProxy(HTTP_PROXY_OPTIONS options);

    /**
     * Set logging level (FULL, API_ONLY, DISABLED). Can be changed at any time.
     * 
     * @param logLevel The logging level. Default: DISABLED
     */
    public void SetLogging(IOTC_LOGGING logLevel);

    /**
     * Disconnect device.
     * 
     * @param callback Callback executing when device successfully disconnect.
     * 
     * @throws IoTCentralException if disconnection fails
     */
    public void Disconnect(IoTCCallback callback) throws IoTCentralException;

    /**
     * Connect device.
     * 
     * @throws IoTCentralException if connection fails
     */
    public void Connect() throws IoTCentralException;

    /**
     * Send a telemetry message.
     * 
     * @param payload  The telemetry object. Can include multiple values in a
     *                 flatten object. It can be a map, a POJO or a JSON string
     * @param callback The callback to execute when message is delivered to the hub
     * @throws IoTCentralException if connection is dropped
     */
    public void SendTelemetry(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Send a state message
     * 
     * @param payload  The state object. Can include multiple values in a flatten
     *                 object. It can be a map, a POJO or a JSON string
     * @param callback The callback to execute when message is delivered to the hub
     * @throws IoTCentralException if connection is dropped
     */
    public void SendState(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Send events
     * 
     * @param payload  The event object. Can include multiple events in a flatten
     *                 object. It can be a map, a POJO or a JSON string
     * @param callback The callback to execute when message is delivered to the hub
     * @throws IoTCentralException if connection is dropped
     */
    public void SendEvent(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Send update values to properties.
     * 
     * @param payload  The property object. Can include multiple values in a flatten
     *                 object. It can be a map, a POJO or a JSON string. If property
     *                 is sent in the form {propertyName:{value:"value"}} and
     *                 propertyName is the name of a command, then it sends updates
     *                 to specific command tile in IoTCentral( e.g. command
     *                 progress)
     * @param callback The callback to execute when message is delivered to the hub
     * @throws IoTCentralException if connection is dropped
     */
    public void SendProperty(Object payload, IoTCCallback callback) throws IoTCentralException;

    /**
     * Listen to events.
     * 
     * @param event    The event to listen to.
     * @param callback The callback to execute when the event is triggered
     */
    public void on(IOTC_EVENTS event, IoTCCallback callback);

}