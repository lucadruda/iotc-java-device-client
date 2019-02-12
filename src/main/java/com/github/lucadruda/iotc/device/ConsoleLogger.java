// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.enums.IOTC_LOGGING;

public class ConsoleLogger implements ILogger {

    private IOTC_LOGGING logLevel = IOTC_LOGGING.DISABLED;

    @Override
    public void Log(String message) {
        if (this.logLevel != IOTC_LOGGING.DISABLED) {
            System.out.println(message);
        }
    }

    @Override
    public void SetLevel(IOTC_LOGGING level) {
        this.logLevel = level;
    }

}