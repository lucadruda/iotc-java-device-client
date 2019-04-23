// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.callbacks.CommandCallback;
import com.github.lucadruda.iotc.device.callbacks.ConnectionStatus;
import com.github.lucadruda.iotc.device.callbacks.DeviceTwinStatusCallback;
import com.github.lucadruda.iotc.device.callbacks.EventCallback;
import com.github.lucadruda.iotc.device.callbacks.IoTCCallback;
import com.github.lucadruda.iotc.device.callbacks.SettingsCallback;
import com.github.lucadruda.iotc.device.enums.*;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Property;

public class IoTCClient implements IIoTCClient {

    final int DEFAULT_EXPIRATION = 21600;
    final String DPS_DEFAULT_ENDPOINT = "global.azure-devices-provisioning.net";

    private String endpoint = DPS_DEFAULT_ENDPOINT;
    private ILogger logger;
    private String id;
    private String scopeId;
    private IOTC_CONNECT authenticationType;
    private String sasKey;
    private IoTCentralCert certificate;
    private IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private DeviceClient deviceClient;

    private HashMap<IOTC_EVENTS, List<IoTCCallback>> callbacks;

    /**
     * 
     * @param id                 The device Id
     * @param scopeId            Scope Id of the application
     * @param authenticationType Type of authentication: It can be Group symmetric
     *                           key, Device SAS key or x.509
     * @param options            Value for authentication: keys for symmetric and
     *                           SAS key authentication or x.509 certificate
     */
    public IoTCClient(String id, String scopeId, IOTC_CONNECT authenticationType, Object options) {
        this.logger = new ConsoleLogger();
        this.id = id;
        this.scopeId = scopeId;
        this.authenticationType = authenticationType;
        if (this.authenticationType == IOTC_CONNECT.SYMM_KEY || this.authenticationType == IOTC_CONNECT.DEVICE_KEY) {
            this.sasKey = (String) options;
        } else if (this.authenticationType == IOTC_CONNECT.X509_CERT) {
            this.certificate = (IoTCentralCert) options;
        }
        this.callbacks = new HashMap<IOTC_EVENTS, List<IoTCCallback>>();
        this.callbacks.put(IOTC_EVENTS.Command, new ArrayList<IoTCCallback>());
        this.callbacks.put(IOTC_EVENTS.SettingsUpdated, new ArrayList<IoTCCallback>());
        this.callbacks.put(IOTC_EVENTS.ConnectionStatus, new ArrayList<IoTCCallback>());
    }

    /**
     * 
     * @param id                 The device Id
     * @param scopeId            Scope Id of the application
     * @param authenticationType Type of authentication: It can be Group symmetric
     *                           key, Device SAS key or x.509
     * @param options            Value for authentication: keys for symmetric and
     *                           SAS key authentication or x.509 certificate
     * @param logger             A custom logger implementing the ILogger interface
     */
    public IoTCClient(String id, String scopeId, IOTC_CONNECT authenticationType, Object options, ILogger logger) {
        this(id, scopeId, authenticationType, options);
        this.logger = logger;
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
        this.endpoint = endpoint;
        this.logger.Log("Endpoint changed to: " + endpoint);
    }

    @Override
    public void SetProxy(HTTP_PROXY_OPTIONS options) {
        this.logger.Log("Setting proxy to " + options.host_address);
        System.setProperty("http.proxyHost", options.host_address);
        System.setProperty("http.proxyPort", String.valueOf(options.port));
        System.setProperty("http.proxyUser", options.username);
        System.setProperty("http.proxyPassword", options.password);
        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(options.username, options.password.toCharArray());
            }
        });
        this.logger.Log("Proxy has been set");
    }

    @Override
    public void Disconnect(IoTCCallback callback) throws IoTCentralException {
        try {
            this.deviceClient.closeNow();
            callback.Exec("Disconnected");
        } catch (IOException e) {
            throw new IoTCentralException("Failed to disconnect: " + e.getMessage());
        }
    }

    /**
     * Register a device in IoTCentral using authentication provided at construction time
     * @return DeviceClient instance
     * @throws IoTCentralException
     */
    private DeviceClient Register() throws IoTCentralException {
        if (this.authenticationType == IOTC_CONNECT.SYMM_KEY) {
            return new SasAuthentication(this.endpoint, this.protocol, this.id, this.scopeId, this.logger)
                    .RegisterWithSaSKey(this.sasKey);
        } else if (this.authenticationType == IOTC_CONNECT.DEVICE_KEY) {
            return new SasAuthentication(this.endpoint, this.protocol, this.id, this.scopeId, this.logger)
                    .RegisterWithDeviceKey(this.sasKey);
        }
        return new CertAuthentication(this.endpoint, this.protocol, this.id, this.scopeId, this.certificate,
                this.logger).Register();
    }

    @Override
    public void Connect() throws IoTCentralException {
        this.deviceClient = this.Register();
        try {
            this.deviceClient.open();
            this.deviceClient.registerConnectionStatusChangeCallback(
                    new ConnectionStatus(this.callbacks.get(IOTC_EVENTS.ConnectionStatus)), null);
            this.deviceClient.startDeviceTwin(new DeviceTwinStatusCallback(null), null,
                    new SettingsCallback(this.callbacks.get(IOTC_EVENTS.SettingsUpdated)), null);
            this.deviceClient.subscribeToDeviceMethod(new CommandCallback(this.callbacks.get(IOTC_EVENTS.Command)),
                    null, new EventCallback(null, this.logger), null);
        } catch (IOException e) {
            throw new IoTCentralException(e.getMessage());
        }
        this.logger.Log("Device connected");

    }

    @Override
    public void SendTelemetry(Object payload, IoTCCallback callback) {
        this.SendEvent(payload, callback);
    }

    @Override
    public void SendState(Object payload, IoTCCallback callback) {
        this.SendEvent(payload, callback);
    }

    @Override
    public void SendEvent(Object payload, IoTCCallback callback) {
        String msgStr;
        if (payload instanceof String) {
            msgStr = (String) payload;
        } else {
            Gson gson = new GsonBuilder().create();
            msgStr = gson.toJson(payload);
        }
        Message msg = new Message(msgStr);
        msg.setContentType("application/json");
        msg.setMessageId(UUID.randomUUID().toString());
        EventCallback evCallback = new EventCallback(callback, this.logger);
        this.deviceClient.sendEventAsync(msg, evCallback, msg);
    }

    @Override
    public void SendProperty(Object payload, IoTCCallback callback) throws IoTCentralException {
        HashSet<Property> set = new HashSet<>();
        String msgStr;
        if (payload instanceof String) {
            msgStr = (String) payload;
        } else {
            Gson gson = new GsonBuilder().create();
            msgStr = gson.toJson(payload);
        }
        JsonObject json = (JsonObject) new JsonParser().parse(msgStr);
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement val = entry.getValue();
            set.add(new Property(entry.getKey(), val));
        }
        try {
            this.deviceClient.sendReportedProperties(set);
            if (callback != null) {
                callback.Exec("Properties sent");
            }
        } catch (Exception ex) {
            throw new IoTCentralException(ex.getMessage());
        }
    }

    @Override
    public void on(IOTC_EVENTS event, IoTCCallback callback) {
        switch (event) {
        case ConnectionStatus:
            this.callbacks.get(IOTC_EVENTS.ConnectionStatus).add(callback);
            break;
        case SettingsUpdated:
            this.callbacks.get(IOTC_EVENTS.SettingsUpdated).add(callback);
            break;
        case Command:
            this.callbacks.get(IOTC_EVENTS.Command).add(callback);
            break;
        default:
            break;
        }
    }

}