// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.callbacks.CommandCallback;
import com.github.lucadruda.iotc.device.enums.*;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;
import com.github.lucadruda.iotc.device.models.CommandResponse;
import com.github.lucadruda.iotc.device.models.IoTCCommand;
import com.github.lucadruda.iotc.device.models.IoTCProperty;
import com.github.lucadruda.iotc.device.models.PropertyResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.microsoft.azure.sdk.iot.deps.serializer.FileUploadCompletionNotification;
import com.microsoft.azure.sdk.iot.deps.serializer.FileUploadSasUriRequest;
import com.microsoft.azure.sdk.iot.deps.serializer.FileUploadSasUriResponse;
import com.microsoft.azure.sdk.iot.deps.serializer.MethodParser;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionState;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStateCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Property;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.TwinPropertyCallBack;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.github.lucadruda.iotc.device.callbacks.IoTCCallback;
import com.github.lucadruda.iotc.device.callbacks.PropertiesCallback;

public class IoTCClient implements IIoTCClient {

    private ILogger logger;
    private String deviceId;
    private String scopeId;
    private String modelId;
    private DeviceProvision deviceProvision;
    private IOTC_CONNECT authenticationType;
    private String sasKey;
    private IoTCentralCert certificate;
    private IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private DeviceClient deviceClient;

    private HashMap<IOTC_EVENTS, IoTCCallback> events;

    private boolean connected;

    /**
     * 
     * @param deviceId           The device Id
     * @param scopeId            Scope Id of the application
     * @param authenticationType Type of authentication: It can be Group symmetric
     *                           key, Device SAS key or x.509
     * @param options            Value for authentication: keys for symmetric and
     *                           SAS key authentication or x.509 certificate
     * @param logger             A custom logger implementing the ILogger interface
     */
    public IoTCClient(String deviceId, String scopeId, IOTC_CONNECT authenticationType, Object options,
            ILogger logger) {
        this.connected = false;
        this.deviceId = deviceId;
        this.logger = logger;
        this.scopeId = scopeId;
        this.authenticationType = authenticationType;
        if (this.authenticationType == IOTC_CONNECT.SYMM_KEY || this.authenticationType == IOTC_CONNECT.DEVICE_KEY) {
            this.sasKey = (String) options;
        } else if (this.authenticationType == IOTC_CONNECT.X509_CERT) {
            this.certificate = (IoTCentralCert) options;
        }
        this.events = new HashMap<IOTC_EVENTS, IoTCCallback>();
        this.deviceProvision = new DeviceProvision(deviceId, scopeId, authenticationType, options, logger);
    }

    public IoTCClient(String deviceId, String scopeId, IOTC_CONNECT authenticationType, Object options) {
        this(deviceId, scopeId, authenticationType, options, new ConsoleLogger());
    }

    /**
     * Returns the model Id
     * 
     * @return the modelId
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Set the model Id
     */
    public void SetModelId(String modelId) {
        this.deviceProvision.setIoTCModelId(modelId);
    }

    /**
     * @param logLevel the logger to set
     */
    public void SetLogging(IOTC_LOGGING logLevel) {
        this.logger.SetLevel(logLevel);
    }

    @Override
    public void SetProtocol(IOTC_PROTOCOL transport) {
        this.protocol = IotHubClientProtocol.valueOf(transport.name());
        this.logger.Log("Transport set to " + transport);
    }

    @Override
    public void SetGlobalEndpoint(String endpoint) {
        this.deviceProvision.setEndpoint(endpoint);
        this.logger.Log("Endpoint changed to: " + endpoint);
    }

    @Override
    public void Disconnect() throws IoTCentralException {
        try {
            this.deviceClient.closeNow();
        } catch (IOException ex) {
            throw new IoTCentralException(ex.getMessage());
        }

    }

    @Override
    public void Connect() throws IoTCentralException {
        this.Connect(30);

    }

    @Override
    public void Connect(Integer timeout) throws IoTCentralException {
        this.deviceClient = this.deviceProvision.register();
        this.listenToConnectionState();
        try {
            this.deviceClient.open();
            this.listenToProperties();
            this.listenToCommands();
            this.listenToC2D();

        } catch (IOException e) {
            throw new IoTCentralException(e.getMessage());
        }
        this.logger.Log("Device connected");

    }

    private void listenToConnectionState() {
        IotHubConnectionStatusChangeCallback connectionStateCallback = new IotHubConnectionStatusChangeCallback() {
            @Override
            public void execute(IotHubConnectionStatus state, IotHubConnectionStatusChangeReason statusChangeReason,
                    Throwable throwable, Object callbackContext) {
                IoTCClient client = (IoTCClient) callbackContext;
                if (state == IotHubConnectionStatus.CONNECTED) {
                    client.SetConnectionState(true);
                } else {
                    client.SetConnectionState(false);
                }
            }
        };
        this.deviceClient.registerConnectionStatusChangeCallback(connectionStateCallback, this);
    }

    private void listenToCommands() throws IOException, IllegalArgumentException {
        this.deviceClient.subscribeToDeviceMethod(new DeviceMethodCallback() {
            @Override
            public DeviceMethodData call(String methodName, Object methodData, Object context) {
                IoTCClient client = (IoTCClient) context;
                if (client.events.containsKey(IOTC_EVENTS.Commands)) {
                    try {
                        String payload = new String((byte[]) methodData, "UTF-8");
                        CommandResponse resp = (name, message) -> {
                            try {
                                client.SendProperty(String.format("{\"%s\":\"%s\"}", name, message));
                                return IOTC_COMMAND_RESPONSE.SUCCESS;
                            } catch (IoTCentralException ex) {
                                return IOTC_COMMAND_RESPONSE.ERROR;
                            }
                        };
                        return ((CommandCallback) client.events.get(IOTC_EVENTS.Commands))
                                .exec(new IoTCCommand(methodName, payload, resp));
                    } catch (Exception ex) {
                        return new DeviceMethodData(500, "Failure");
                    }
                } else {
                    return new DeviceMethodData(200, "Received");
                }
            }
        }, this, new IotHubEventCallback() {
            @Override
            public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
                // no-op
            }
        }, null);
    }

    private void listenToProperties() throws IOException {
        TwinPropertyCallBack propCb = new TwinPropertyCallBack() {
            @Override
            public void TwinPropertyCallBack(Property property, Object context) {
                IoTCClient client = (IoTCClient) context;
                if (client.events.containsKey(IOTC_EVENTS.Properties)) {
                    IoTCCallback callback = client.events.get(IOTC_EVENTS.Properties);
                    if (callback instanceof PropertiesCallback) {
                        JsonObject jobj = (JsonObject) new JsonParser().parse(property.getValue().toString());
                        JsonPrimitive primitive = jobj.get("value").getAsJsonPrimitive();
                        Object value = primitive.getAsString();
                        if (primitive.isBoolean()) {
                            value = primitive.getAsBoolean();
                        } else if (primitive.isNumber()) {
                            value = primitive.getAsNumber();
                        }
                        PropertyResponse response = (syncValue) -> {
                            try {
                                client.SendProperty(syncValue);
                            } catch (IoTCentralException ex) {
                                client.logger.Debug(ex.getMessage(), "Twin");
                            }
                        };
                        ((PropertiesCallback) callback)
                                .exec(new IoTCProperty(property.getKey(), value, property.getVersion(), response));
                    }

                } else {
                    return; // no-op
                }
            }
        };
        this.deviceClient.startDeviceTwin(new IotHubEventCallback() {
            public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
                // no-op
            };
        }, this, propCb, this);
    }

    private void listenToC2D() {
        MessageCallback callback = new MessageCallback() {
            @Override
            public IotHubMessageResult execute(Message message, Object callbackContext) {
                IoTCClient client = (IoTCClient) callbackContext;
                if (client.events.containsKey(IOTC_EVENTS.Commands)) {
                    try {
                        String payload = new String(message.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET);
                        String methodName = null;
                        MessageProperty[] props = message.getProperties();
                        for (MessageProperty messageProperty : props) {
                            if (messageProperty.getName().equals("method-name")) {
                                methodName = messageProperty.getValue().split(":")[1];
                            }
                        }
                        ((CommandCallback) client.events.get(IOTC_EVENTS.Commands))
                                .exec(new IoTCCommand(methodName, payload));
                        return IotHubMessageResult.COMPLETE;
                    } catch (Exception ex) {
                        if (client.protocol != IotHubClientProtocol.MQTT
                                || client.protocol != IotHubClientProtocol.MQTT_WS) {
                            return IotHubMessageResult.REJECT;
                        }
                        return IotHubMessageResult.COMPLETE;
                    }
                } else {
                    return IotHubMessageResult.COMPLETE;
                }
            }
        };
        this.deviceClient.setMessageCallback(callback, this);
    }

    @Override
    public void SendTelemetry(String payload) throws IoTCentralException {
        this.SendTelemetry(payload, null);
    }

    @Override
    public void SendTelemetry(Object payload) throws IoTCentralException {
        Gson gson = new GsonBuilder().create();
        this.SendTelemetry(gson.toJson(payload), null);
    }

    @Override
    public void SendTelemetry(Object payload, String properties) throws IoTCentralException {
        Gson gson = new GsonBuilder().create();
        this.SendTelemetry(gson.toJson(payload), properties);
    }

    @Override
    public void SendTelemetry(String payload, Object properties) throws IoTCentralException {
        Gson gson = new GsonBuilder().create();
        this.SendTelemetry(payload, gson.toJson(properties));
    }

    @Override
    public void SendTelemetry(Object payload, Object properties) throws IoTCentralException {
        Gson gson = new GsonBuilder().create();
        this.SendTelemetry(gson.toJson(payload), gson.toJson(properties));
    }

    @Override
    public void SendTelemetry(String payload, String properties) throws IoTCentralException {
        if (this.deviceClient == null && (payload == null | payload.isEmpty())) {
            return;
        }
        Message msg = new Message(payload);
        msg.setContentTypeFinal("application/json");
        msg.setMessageId(UUID.randomUUID().toString());
        if (properties != null && !properties.isEmpty()) {
            JsonObject json = (JsonObject) new JsonParser().parse(properties);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                JsonElement val = entry.getValue();
                msg.setProperty(entry.getKey(), val.getAsString());
            }
        }
        try {
            this.deviceClient.sendEventAsync(msg, null, msg);
        } catch (Exception ex) {
            throw new IoTCentralException(ex.getMessage());
        }
    }

    public void SendProperty(String payload) throws IoTCentralException {
        if (this.deviceClient == null && (payload == null | payload.isEmpty())) {
            return;
        }
        HashSet<Property> set = new HashSet<>();
        JsonObject json = (JsonObject) new JsonParser().parse(payload);
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement val = entry.getValue();
            set.add(new Property(entry.getKey(), val));
        }
        try {
            this.deviceClient.sendReportedProperties(set);
        } catch (Exception ex) {
            throw new IoTCentralException(ex.getMessage());
        }
    }

    public void SendProperty(Object payload) throws IoTCentralException {
        Gson gson = new GsonBuilder().create();
        this.SendProperty(gson.toJson(payload));
    }

    @Override
    public void on(IOTC_EVENTS event, IoTCCallback callback) {
        switch (event) {
            case ConnectionStatus:
                if (!this.events.containsKey(IOTC_EVENTS.ConnectionStatus)) {
                    this.events.put(IOTC_EVENTS.ConnectionStatus, callback);
                } else {
                    this.events.replace(IOTC_EVENTS.ConnectionStatus, callback);
                }
                break;
            case Properties:
                if (!this.events.containsKey(IOTC_EVENTS.Properties)) {
                    this.events.put(IOTC_EVENTS.Properties, callback);
                } else {
                    this.events.replace(IOTC_EVENTS.Properties, callback);
                }
                break;
            case Commands:
                if (!this.events.containsKey(IOTC_EVENTS.Commands)) {
                    this.events.put(IOTC_EVENTS.Commands, callback);
                } else {
                    this.events.replace(IOTC_EVENTS.Commands, callback);
                }
                break;
            default:
                break;
        }
    }

    /**
     * @return the logger
     */
    public ILogger getLogger() {
        return this.logger;
    }

    /**
     * @return the id
     */
    public String getDeviceId() {
        return this.deviceId;
    }

    /**
     * @return the scopeId
     */
    public String getScopeId() {
        return this.scopeId;
    }

    /**
     * @return the authenticationType
     */
    public IOTC_CONNECT getAuthenticationType() {
        return this.authenticationType;
    }

    /**
     * @return the sasKey
     */
    public String getSasKey() {
        return sasKey;
    }

    /**
     * @return the certificate
     */
    public IoTCentralCert getCertificate() {
        return certificate;
    }

    /**
     * @return the protocol
     */
    public IotHubClientProtocol getProtocol() {
        return protocol;
    }

    /**
     * @return the deviceClient
     */
    public DeviceClient getDeviceClient() {
        return deviceClient;
    }

    @Override
    public boolean IsConnected() {
        return this.connected;
    }

    public void SetConnectionState(boolean state) {
        this.connected = state;
    }

    @Override
    public void FetchTwin() throws IoTCentralException {
        try {
            this.deviceClient.getDeviceTwin();
        } catch (IOException ex) {
            throw new IoTCentralException(ex.getMessage());
        }

    }

    @Override
    public boolean UploadFile(String fileName, File file) throws IoTCentralException {
        return this.UploadFile(fileName, file, null);
    }

    @Override
    public boolean UploadFile(String fileName, File file, String encoding) throws IoTCentralException {
        FileUploadSasUriResponse response;
        try {
            response = this.deviceClient.getFileUploadSasUri(new FileUploadSasUriRequest(fileName));
        } catch (IOException | URISyntaxException ex) {
            throw new IoTCentralException(ex.getMessage());
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            CloudBlockBlob blob = new CloudBlockBlob(response.getBlobUri());
            blob.upload(fileInputStream, file.length());
            FileUploadCompletionNotification completionNotification = new FileUploadCompletionNotification(
                    response.getCorrelationId(), true);
            this.deviceClient.completeFileUpload(completionNotification);
            return true;
        } catch (Exception ex) {
            FileUploadCompletionNotification completionNotification = new FileUploadCompletionNotification(
                    response.getCorrelationId(), false);
            try {
                this.deviceClient.completeFileUpload(completionNotification);
                return false;
            } catch (IOException e) {
                throw new IoTCentralException(e.getMessage());
            }
        }
    }

}