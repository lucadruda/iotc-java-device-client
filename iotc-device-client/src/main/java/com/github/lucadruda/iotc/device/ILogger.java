// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.enums.IOTC_LOGGING;

public interface ILogger {
    public void Log(String message);

    public void Log(String message, String tag);

    public void Debug(String message);

    public void Debug(String message, String tag);

    public void SetLevel(IOTC_LOGGING level);
}