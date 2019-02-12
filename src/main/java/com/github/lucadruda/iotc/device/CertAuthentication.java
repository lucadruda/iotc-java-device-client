// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import java.net.URISyntaxException;
import java.util.LinkedList;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.provisioning.device.ProvisioningDeviceClientStatus;
import com.microsoft.azure.sdk.iot.provisioning.device.*;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProvider;
import com.microsoft.azure.sdk.iot.provisioning.security.exceptions.SecurityProviderException;
import com.microsoft.azure.sdk.iot.provisioning.security.hsm.SecurityProviderX509Cert;

public class CertAuthentication {

    final int MAX_TIME_TO_WAIT_FOR_REGISTRATION = 10000;

    private String endpoint;
    private String id;
    private String scopeId;
    private String cert;
    private String key;
    private LinkedList<String> chain;
    private ILogger logger;
    private IotHubClientProtocol protocol;

    public CertAuthentication(String endpoint, IotHubClientProtocol protocol, String id, String scopeId,
            IoTCentralCert certificate, ILogger logger) {
        this.endpoint = endpoint;
        this.id = id;
        this.scopeId = scopeId;
        // this.certificate = certificate;
        this.logger = logger;
        this.protocol = protocol;
        this.cert = certificate.certificate;
        this.key = certificate.privateKey;
        this.chain = new LinkedList<String>();
        this.chain.add(certificate.root);
        if (certificate.intermediate != null) {
            this.chain.add(certificate.intermediate);
        }

    }

    static class ProvisioningStatus {
        ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationInfoClient = new ProvisioningDeviceClientRegistrationResult();
        Exception exception;
    }

    static class ProvisioningDeviceClientRegistrationCallbackImpl
            implements ProvisioningDeviceClientRegistrationCallback {
        @Override
        public void run(ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationResult,
                Exception exception, Object context) {
            if (context instanceof ProvisioningStatus) {
                ProvisioningStatus status = (ProvisioningStatus) context;
                status.provisioningDeviceClientRegistrationInfoClient = provisioningDeviceClientRegistrationResult;
                status.exception = exception;
            } else {
                System.out.println("Received unknown context");
            }
        }
    }

    public DeviceClient Register() {
        ProvisioningDeviceClient provisioningDeviceClient = null;
        try {
            ProvisioningStatus provisioningStatus = new ProvisioningStatus();

            SecurityProvider securityProviderX509 = new SecurityProviderX509Cert(this.cert, this.key, this.chain);
            provisioningDeviceClient = ProvisioningDeviceClient.create(this.endpoint, this.scopeId,
                    ProvisioningDeviceClientTransportProtocol.valueOf(this.protocol.name()), securityProviderX509);

            provisioningDeviceClient.registerDevice(new ProvisioningDeviceClientRegistrationCallbackImpl(),
                    provisioningStatus);

            while (provisioningStatus.provisioningDeviceClientRegistrationInfoClient
                    .getProvisioningDeviceClientStatus() != ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {
                if (provisioningStatus.provisioningDeviceClientRegistrationInfoClient
                        .getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ERROR
                        || provisioningStatus.provisioningDeviceClientRegistrationInfoClient
                                .getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_DISABLED
                        || provisioningStatus.provisioningDeviceClientRegistrationInfoClient
                                .getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_FAILED)

                {
                    String msg = "Error provisioning device. " + provisioningStatus.exception.getMessage();
                    this.logger.Log(msg);
                    break;
                }
                this.logger.Log("Waiting for Provisioning Service to register");
                Thread.sleep(MAX_TIME_TO_WAIT_FOR_REGISTRATION);
            }

            if (provisioningStatus.provisioningDeviceClientRegistrationInfoClient
                    .getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {

                // connect to iothub
                String iotHubUri = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri();
                String deviceId = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId();
                return new DeviceClient(String.format("HostName=%s;DeviceId=%s;x509=true", iotHubUri, deviceId),
                        this.protocol, this.cert, false, this.key, false);
            } else {
                throw new ProvisioningDeviceClientException("Wrong provisioning status");
            }
        } catch (URISyntaxException | ProvisioningDeviceClientException | InterruptedException
                | SecurityProviderException e) {
            this.logger.Log("Provisioning Device Client threw an exception" + e.getMessage());
            if (provisioningDeviceClient != null) {
                provisioningDeviceClient.closeNow();
            }
            return null;
        }
    }

}