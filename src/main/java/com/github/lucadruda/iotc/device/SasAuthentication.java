// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.NoRouteToHostException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.sdk.iot.deps.util.Base64;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.auth.Signature;
import com.github.lucadruda.iotc.device.enums.IOTC_CONNECTION_STATE;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;

public class SasAuthentication {

    final int DEFAULT_EXPIRATION = 21600; // 6 hours

    private IoTCClient client;

    public SasAuthentication(IoTCClient client) {
        this.client = client;

    }

    public DeviceClient RegisterWithSaSKey(String symKey) throws IoTCentralException {
        if (this.client.getScopeId() == null || this.client.getScopeId().length() == 0 || symKey == null
                || symKey.length() == 0 || this.client.getId() == null || this.client.getId().length() == 0) {
            throw new IoTCentralException("Wrong credentials values");
        }
        return this.RegisterWithDeviceKey(this.ComputeKey(symKey, this.client.getId()));
    }

    public DeviceClient RegisterWithDeviceKey(String deviceKey) throws IoTCentralException {
        if (this.client.getScopeId() == null || this.client.getScopeId().length() == 0 || deviceKey == null
                || deviceKey.length() == 0 || this.client.getId() == null || this.client.getId().length() == 0) {
            throw new IoTCentralException("Wrong credentials values");
        }
        long time = (System.currentTimeMillis() / 1000 | 0) + DEFAULT_EXPIRATION;

        Signature sig = new Signature(this.client.getScopeId() + "%2fregistrations%2f" + this.client.getId(), time,
                deviceKey);
        String sasKey = String.format(
                "SharedAccessSignature sr=%s%%2fregistrations%%2f%s&sig=%s&skn=registration&se=%s",
                this.client.getScopeId(), this.client.getId(), sig.toString(), time);
        String operationId = this.RequestRegistration(sasKey);
        String connectionString = this.GetAssignment(deviceKey, sasKey, operationId);
        DeviceClient client;
        try {
            client = new DeviceClient(connectionString, this.client.getProtocol());
            return client;
        } catch (IllegalArgumentException | URISyntaxException e) {
            throw new IoTCentralException("Invalid connection string: " + connectionString);
        }

    }

    private String RequestRegistration(String sasKey) throws IoTCentralException {
        try {
            URL url = new URL(
                    String.format("https://%s/%s/registrations/%s/register?api-version=%s", this.client.getEndpoint(),
                            this.client.getScopeId(), this.client.getId(), this.client.getApiVersion()));
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Host", this.client.getEndpoint());
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("UserAgent", "prov_device_client/1.0");
            con.setRequestProperty("Authorization", sasKey);
            con.setDoOutput(true);
            DataOutputStream os = new DataOutputStream(con.getOutputStream());
            if (this.client.getModelId() != null || this.client.getModelId().length() != 0) {
                os.writeBytes(String.format("{\"registrationId\":\"%s\",\"data\":{\"iotcModelId\":\"%s\"}}",
                        this.client.getId(), this.client.getModelId()));
            } else {
                os.writeBytes(String.format("{\"registrationId\":\"%s\"}", this.client.getId()));
            }
            os.flush();
            os.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            JsonObject data = (JsonObject) new JsonParser().parse(content.toString());
            return data.get("operationId").getAsString();
        } catch (Exception ex) {
            if (ex instanceof UnknownHostException) {
                // network not available
                throw new IoTCentralException(IOTC_CONNECTION_STATE.NO_NETWORK);
            }
            throw new IoTCentralException(ex.getMessage());
        }
    }

    private String GetAssignment(String deviceKey, String sasKey, String operationId) throws IoTCentralException {
        try {
            URL url = new URL(String.format("https://%s/%s/registrations/%s/operations/%s?api-version=%s",
                    this.client.getEndpoint(), this.client.getScopeId(), this.client.getId(), operationId,
                    this.client.getApiVersion()));
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Host", this.client.getEndpoint());
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("UserAgent", "prov_device_client/1.0");
            con.setRequestProperty("Authorization", sasKey);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            JsonObject data = (JsonObject) new JsonParser().parse(content.toString());
            if (data.get("status").getAsString().equals("assigned")) {
                this.client.getLogger().Log("Device registered");
                JsonObject registrationState = (JsonObject) data.get("registrationState");
                return String.format("HostName=%s;DeviceId=%s;SharedAccessKey=%s",
                        registrationState.get("assignedHub").getAsString(),
                        registrationState.get("deviceId").getAsString(), deviceKey);
            } else if (data.get("status").getAsString().equals("assigning")) {
                this.client.getLogger().Log("Waiting for registration");
                Thread.sleep(2000);
                return this.GetAssignment(deviceKey, sasKey, operationId);
            } else {
                throw new Exception("Wrong request");
            }
        } catch (Exception ex) {
            if (ex instanceof NoRouteToHostException) {
                throw new IoTCentralException(IOTC_CONNECTION_STATE.NO_NETWORK);
            }
            throw new IoTCentralException(ex.getMessage());
        }
    }

    private String ComputeKey(String masterKey, String registrationId) {
        try {
            Mac sha256_hmac = Mac.getInstance("HmacSHA256");
            byte[] keyBytes = Base64.decodeBase64Local(new String(masterKey).getBytes(StandardCharsets.UTF_8));
            sha256_hmac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] regBytes = registrationId.getBytes(StandardCharsets.UTF_8);
            byte[] res = sha256_hmac.doFinal(regBytes);
            return Base64.encodeBase64StringLocal(res);
        } catch (Exception ex) {
            return null;
        }
    }
}