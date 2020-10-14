// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import java.io.File;

import com.github.lucadruda.iotc.device.callbacks.IoTCCallback;
import com.github.lucadruda.iotc.device.enums.*;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;

public interface IIoTCClient {
    /**
     * 
     * @param modelId IoT Central model Id for automatic approval process
     */
    public void SetModelId(String modelId);

    /**
     * Set global endpoint for DPS provisioning
     * 
     * @param endpoint hostname without protocol
     */
    public void SetGlobalEndpoint(String endpoint);

    /**
     * Disconnect device. Client cannot be reused after disconnect!!!
     * 
     * @throws IoTCentralException If disconnection fails
     * 
     */
    public void Disconnect() throws IoTCentralException;

    /**
     * Connect the device
     * 
     * @throws IoTCentralException If connection fails
     */
    public void Connect() throws IoTCentralException;

    /**
     * Connect the device
     * 
     * @param timeout Timeout in seconds before forcing connection stop
     * 
     * @throws IoTCentralException If connection fails
     */
    public void Connect(Integer timeout) throws IoTCentralException;

    /**
     * 
     * @param payload Message to send: can be any type (usually json) or a
     *                collection of messages
     * @throws IoTCentralException If telemetry delivery failed
     * 
     */
    public void SendTelemetry(Object payload) throws IoTCentralException;

    /**
     * 
     * @param payload Message to send: can be any type (usually json) or a
     *                collection of messages
     * @throws IoTCentralException If telemetry delivery failed
     */
    public void SendTelemetry(String payload) throws IoTCentralException;

    /**
     * 
     * @param payload    Message to send: can be any type (usually json) or a
     *                   collection of messages
     * @param properties Properties to be added to the message (JSON format)
     * @throws IoTCentralException If telemetry delivery failed
     */
    public void SendTelemetry(Object payload, Object properties) throws IoTCentralException;

    /**
     * 
     * @param payload    Message to send: can be any type (usually json) or a
     *                   collection of messages
     * @param properties Properties to be added to the message (JSON format)
     * @throws IoTCentralException If telemetry delivery failed
     */
    public void SendTelemetry(String payload, Object properties) throws IoTCentralException;

    /**
     * 
     * @param payload    Message to send: can be any type (usually json) or a
     *                   collection of messages
     * @param properties Properties to be added to the message (JSON format)
     * @throws IoTCentralException If telemetry delivery failed
     */
    public void SendTelemetry(String payload, String properties) throws IoTCentralException;

    /**
     * 
     * @param payload    Message to send: can be any type (usually json) or a
     *                   collection of messages
     * @param properties Properties to be added to the message (JSON format)
     * @throws IoTCentralException If telemetry delivery failed
     */
    public void SendTelemetry(Object payload, String properties) throws IoTCentralException;

    /**
     * 
     * @param payload Property to send: can be any type (usually json) or a
     *                collection of properties
     * @throws IoTCentralException If property delivery fails
     */
    public void SendProperty(Object payload) throws IoTCentralException;

    /**
     * 
     * @param payload Property to send: can be any type (usually json) or a
     *                collection of properties
     * @throws IoTCentralException If property delivery fails
     */
    public void SendProperty(String payload) throws IoTCentralException;

    public void SetProtocol(IOTC_PROTOCOL transport);

    /**
     * 
     * @param eventName name of the event to listen
     * @param callback  function to execute when event triggers
     */
    public void on(IOTC_EVENTS eventName, IoTCCallback callback);

    public void SetLogging(IOTC_LOGGING loggingLevel);

    public boolean IsConnected();

    public void FetchTwin() throws IoTCentralException;

    public boolean UploadFile(String fileName, File file) throws IoTCentralException;

    public boolean UploadFile(String fileName, File file, String encoding) throws IoTCentralException;

}