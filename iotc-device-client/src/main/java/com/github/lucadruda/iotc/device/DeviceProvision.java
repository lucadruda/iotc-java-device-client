package com.github.lucadruda.iotc.device;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.github.lucadruda.iotc.device.enums.IOTC_CONNECT;
import com.github.lucadruda.iotc.device.enums.IOTC_PROTOCOL;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;
import com.github.lucadruda.iotc.device.models.Storage;
import com.github.lucadruda.iotc.device.models.X509Certificate;
import com.microsoft.azure.sdk.iot.deps.util.Base64;
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

public class DeviceProvision {

    static class ProvisioningStatus {
        ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationInfoClient = new ProvisioningDeviceClientRegistrationResult();
        Exception exception;
    }

    private final static String DPS_DEFAULT_ENDPOINT = "global.azure-devices-provisioning.net";
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
    private String deviceKey;

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
            } else {
                this.deviceKey = (String) options;
                if (this.authenticationType == IOTC_CONNECT.SYMM_KEY
                        && (this.modelId != null && !this.modelId.isEmpty())) {
                    this.deviceKey = ComputeKey((String) this.options, this.deviceId);
                }
                provider = new SecurityProviderSymmetricKey(deviceKey.getBytes(), this.deviceId);
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
                Storage storage = new Storage();
                storage.setHubName(
                        this.provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri());
                storage.setDeviceId(this.deviceId);
                if (this.deviceKey != null && !this.deviceKey.isEmpty()) {
                    storage.setDeviceKey(this.deviceKey);
                } else {
                    storage.setCertificate((X509Certificate) this.options);
                }
                centralStorage.persist(storage);
                return storage.getConnectionString();

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

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}
