package com.github.lucadruda.iotc.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.github.lucadruda.iotc.device.enums.IOTC_CONNECT;
import com.github.lucadruda.iotc.device.enums.IOTC_PROTOCOL;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;
import com.github.lucadruda.iotc.device.models.RegistrationResult;
import com.github.lucadruda.iotc.device.models.Storage;
import com.github.lucadruda.iotc.device.models.SubmitRegistrationResponse;
import com.github.lucadruda.iotc.device.models.X509Certificate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.auth.Signature;
import com.microsoft.azure.sdk.iot.provisioning.device.AdditionalData;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClient;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientRegistrationCallback;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientRegistrationResult;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientStatus;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientTransportProtocol;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProvider;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProviderSymmetricKey;
import com.microsoft.azure.sdk.iot.provisioning.security.hsm.SecurityProviderX509Cert;

import android.util.Base64;

public class DeviceProvision {

    static class ProvisioningStatus {
        ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationInfoClient = new ProvisioningDeviceClientRegistrationResult();
        Exception exception;
    }

    private final static String DPS_DEFAULT_ENDPOINT = "global.azure-devices-provisioning.net";
    private final int DEFAULT_EXPIRATION = 21600;
    private static final int MAX_TIME_TO_WAIT_FOR_REGISTRATION = 10000; // in milli seconds
    private final static ProvisioningDeviceClientTransportProtocol PROVISIONING_DEVICE_CLIENT_TRANSPORT_PROTOCOL = ProvisioningDeviceClientTransportProtocol.MQTT;

    private String modelId;
    private String deviceId;
    private String scopeId;
    private IOTC_CONNECT authenticationType;
    private Object options;
    private String endpoint;
    private ProvisioningDeviceClientTransportProtocol protocol;
    private ILogger logger;
    private ProvisioningStatus provisioningStatus;
    private ICentralStorage centralStorage;
    private byte[] deviceKey;
    private TLSv2SocketFactory socketFactory;

    public DeviceProvision(String deviceId, String scopeId, IOTC_CONNECT authenticationType, Object options,
            ICentralStorage storage, ILogger logger) {
        this(DPS_DEFAULT_ENDPOINT, deviceId, scopeId, authenticationType, options, storage, logger);
    }

    public DeviceProvision(String endpoint, String deviceId, String scopeId, IOTC_CONNECT authenticationType,
            Object options, ICentralStorage storage, ILogger logger) {
        this.endpoint = endpoint;
        this.deviceId = deviceId;
        this.scopeId = scopeId;
        this.authenticationType = authenticationType;
        this.options = options;
        this.protocol = PROVISIONING_DEVICE_CLIENT_TRANSPORT_PROTOCOL;
        this.logger = logger;
        provisioningStatus = new ProvisioningStatus();
        this.centralStorage = storage;
    }

    public void setIoTCModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setProtocol(IOTC_PROTOCOL protocol) {
        this.protocol = ProvisioningDeviceClientTransportProtocol.valueOf(protocol.toString());
    }

    public String register() throws IoTCentralException {
        return register(false);
    }

    public String register(boolean force) throws IoTCentralException {
        SecurityProvider provider;
        String connString;
        try {
            if (!force) {
                connString = this.centralStorage.retrieve().getConnectionString();
                if (connString != null && !connString.isEmpty()) {
                    return connString;
                }
            }
            if (this.authenticationType == IOTC_CONNECT.X509_CERT) {
                X509Certificate x509Certificate = (X509Certificate) options;
                provider = new SecurityProviderX509Cert(x509Certificate.getCertificate(),
                        x509Certificate.getPrivateKey(), new LinkedList<>());
                this.socketFactory = new TLSv2SocketFactory(SSLContextBuilder
                        .buildSSLContext(x509Certificate.getCertificate(), x509Certificate.getPrivateKey()));
            } else {
                this.deviceKey = ((String) options).getBytes();
                if (this.authenticationType == IOTC_CONNECT.SYMM_KEY
                        && (this.modelId != null && !this.modelId.isEmpty())) {
                    try {
                        this.deviceKey = SecurityProviderSymmetricKey
                                .ComputeDerivedSymmetricKey(((String) this.options).getBytes(), this.deviceId);
                    } catch (NoClassDefFoundError e) {
                        this.deviceKey = this.ComputeKeyForAndroid((String) this.options, this.deviceId).getBytes();
                    }
                }
                provider = new SecurityProviderSymmetricKey(this.deviceKey, this.deviceId);
                this.socketFactory = new TLSv2SocketFactory(SSLContextBuilder.buildSSLContext());

            }
            if (this.protocol == ProvisioningDeviceClientTransportProtocol.HTTPS) {
                return this._internalHttpsRegister();
            }
            return this._internalRegister(provider);

        } catch (Exception ex) {
            throw new IoTCentralException(ex.getMessage());
        }
    }

    private String _internalRegister(SecurityProvider provider)
            throws IoTCentralException, IOException, URISyntaxException {
        try {
            ProvisioningDeviceClient provisioningDeviceClient = ProvisioningDeviceClient.create(this.endpoint,
                    this.scopeId, this.protocol, provider);
            AdditionalData dpsPayload = null;
            ProvisioningDeviceClientRegistrationCallback registrationCallback = new ProvisioningDeviceClientRegistrationCallback() {
                @Override
                public void run(ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationResult,
                        Exception e, Object context) {
                    DeviceProvision client = (DeviceProvision) context;
                    client.provisioningStatus.provisioningDeviceClientRegistrationInfoClient = provisioningDeviceClientRegistrationResult;
                    client.provisioningStatus.exception = e;

                }
            };
            if (this.modelId != null && !this.modelId.isEmpty()) {
                dpsPayload = new AdditionalData();
                dpsPayload.setProvisioningPayload(String.format("{\"iotcModelId\":\"%s\"}", this.modelId));
                provisioningDeviceClient.registerDevice(registrationCallback, this, dpsPayload);
            } else {
                provisioningDeviceClient.registerDevice(registrationCallback, this);
            }

            ProvisioningDeviceClientStatus provStatus;

            while ((provStatus = this.provisioningStatus.provisioningDeviceClientRegistrationInfoClient
                    .getProvisioningDeviceClientStatus()) != ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {
                if (provStatus == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ERROR
                        || provStatus == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_DISABLED
                        || provStatus == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_FAILED) {
                    this.provisioningStatus.exception.printStackTrace();
                    throw new IoTCentralException("Registration error, bailing out");
                }
                this.logger.Log("Waiting for Provisioning Service to register."
                        + (provStatus != null ? provStatus.toString() : ""));
                Thread.sleep(MAX_TIME_TO_WAIT_FOR_REGISTRATION);
            }

            if (provStatus == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {
                return _generateConnectionString(
                        this.provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri());
            }
            throw new IoTCentralException(provStatus != null ? provStatus.toString() : "");
        } catch (ProvisioningDeviceClientException |

                InterruptedException ex) {
            throw new IoTCentralException(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IoTCentralException(e.getMessage());
        }
    }

    private String _generateConnectionString(String iothubUri) {
        Storage storage = new Storage();
        storage.setHubName(iothubUri);
        storage.setDeviceId(this.deviceId);
        if (this.deviceKey != null) {
            storage.setDeviceKey(this.deviceKey);
        } else {
            storage.setCertificate((X509Certificate) this.options);
        }
        centralStorage.persist(storage);
        return storage.getConnectionString();
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    private String ComputeKeyForAndroid(String masterKey, String registrationId) {
        try {
            Mac sha256_hmac = Mac.getInstance("HmacSHA256");
            byte[] keyBytes = Base64.decode(new String(masterKey).getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            sha256_hmac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] regBytes = registrationId.getBytes(StandardCharsets.UTF_8);
            byte[] res = sha256_hmac.doFinal(regBytes);
            return Base64.encodeToString(res, Base64.DEFAULT);
        } catch (Exception ex) {
            return null;
        }
    }

    private String _internalHttpsRegister()
            throws IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        String operationId = this.SubmitHttpsRegistration();
        RegistrationResult result;
        while ((result = this.QueryHttpsRegistration(operationId)) == null) {
            Thread.sleep(MAX_TIME_TO_WAIT_FOR_REGISTRATION);
        }
        return _generateConnectionString(result.getAssignedHub());

    }

    private String _getSasKey() {
        long time = (System.currentTimeMillis() / 1000 | 0) + DEFAULT_EXPIRATION;
        Signature sig = new Signature(this.scopeId + "%2fregistrations%2f" + this.deviceId, time,
                new String(this.deviceKey, StandardCharsets.UTF_8));
        return String.format("SharedAccessSignature sr=%s%%2fregistrations%%2f%s&sig=%s&skn=registration&se=%s",
                this.scopeId, this.deviceId, sig.toString(), time);
    }

    private String SubmitHttpsRegistration() throws KeyManagementException, NoSuchAlgorithmException {

        try {
            this.logger.Log("Submitting provisioning request...");
            URL url = new URL(String.format(
                    "https://global.azure-devices-provisioning.net/%s/registrations/%s/register?api-version=2019-03-31",
                    this.scopeId, this.deviceId));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(this.socketFactory);
            String payload = String.format("{\"registrationId\":\"%s\"", this.deviceId);
            if (this.modelId != null && !this.modelId.isEmpty()) {
                payload += (", \"payload\":" + String.format("{\"iotcModelId\":\"%s\"}", this.modelId));
            }
            payload += "}";
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Host", this.endpoint);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("UserAgent", "prov_device_client/1.0");
            connection.setRequestProperty("Authorization", this._getSasKey());
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();
            OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
            osw.write(payload);
            osw.close();
            int status = connection.getResponseCode();
            this.logger.Debug("Provisioning request status: " + status);
            switch (status) {
            case 200:
            case 201:
            case 202:
                this.logger.Log("Provisioning request successfully submitted.");
                this.logger.Log("Waiting for operation to complete...");

                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                Gson gson = new GsonBuilder().create();
                SubmitRegistrationResponse resp = gson.fromJson(sb.toString(), SubmitRegistrationResponse.class);
                return resp.getOperationId();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private RegistrationResult QueryHttpsRegistration(String operationId)
            throws IOException, KeyManagementException, NoSuchAlgorithmException {
        this.logger.Log("Waiting for operation to complete...");
        URL url = new URL(String.format(
                "https://global.azure-devices-provisioning.net/%s/registrations/%s/operations/%s?api-version=2019-03-31",
                this.scopeId, this.deviceId, operationId));
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(this.socketFactory);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Host", this.endpoint);
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("UserAgent", "prov_device_client/1.0");
        connection.setRequestProperty("Authorization", this._getSasKey());
        connection.setDoInput(true);
        connection.connect();
        int status = connection.getResponseCode();

        switch (status) {
        case 200:
        case 201:
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            Gson gson = new GsonBuilder().create();
            SubmitRegistrationResponse resp = gson.fromJson(sb.toString(), SubmitRegistrationResponse.class);
            if (resp.getRegistrationState().getStatus().equals("assigned")) {
                this.logger.Log("Device successfully provisioned. Connecting now.");
                return resp.getRegistrationState();
            }
        }
        return null;
    }

}
