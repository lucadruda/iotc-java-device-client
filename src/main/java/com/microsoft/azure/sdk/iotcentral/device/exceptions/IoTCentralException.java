// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device.exceptions;

import com.microsoft.azure.sdk.iotcentral.device.enums.IOTC_CONNECTION_STATE;

public class IoTCentralException extends Exception {

    private static final long serialVersionUID = 1L;
    private IOTC_CONNECTION_STATE connectionState;

    public IoTCentralException() {
        super();
    }

    public IoTCentralException(String message) {
        super(message);
    }

    public IoTCentralException(IOTC_CONNECTION_STATE connectionState) {
        super();
        this.connectionState = connectionState;
    }

    /**
     * @return the connectionState
     */
    public IOTC_CONNECTION_STATE getConnectionState() {
        return connectionState;
    }

}