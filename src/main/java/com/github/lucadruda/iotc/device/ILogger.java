// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.enums.IOTC_LOGGING;

public interface ILogger {
    public void Log(String message);

    public void SetLevel(IOTC_LOGGING level);
}