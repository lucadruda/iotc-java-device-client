// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.microsoft.azure.sdk.iotcentral.device;

import com.microsoft.azure.sdk.iotcentral.device.enums.IOTC_LOGGING;

public interface ILogger {
    public void Log(String message);

    public void SetLevel(IOTC_LOGGING level);
}