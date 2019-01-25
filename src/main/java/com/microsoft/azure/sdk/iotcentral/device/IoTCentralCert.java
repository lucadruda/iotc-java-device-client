// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device;

public class IoTCentralCert {

    public IoTCentralCert(String certificate, String privateKey, String root) {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.root = root;
    }

    public IoTCentralCert(String certificate, String privateKey, String root, String intermediate) {
        this(certificate, privateKey, root);
        this.intermediate = intermediate;
    }

    public String certificate;
    public String privateKey;
    public String root;
    public String intermediate;
}