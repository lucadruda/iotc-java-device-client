// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.
package com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.enums.IOTC_LOGGING;

public class ConsoleLogger implements ILogger {

    private IOTC_LOGGING logLevel = IOTC_LOGGING.DISABLED;

    @Override
    public void Log(String message) {
        this.Log(message, null);
    }

    @Override
    public void SetLevel(IOTC_LOGGING level) {
        this.logLevel = level;
    }

    @Override
    public void Log(String message, String tag) {
        if (this.logLevel != IOTC_LOGGING.DISABLED) {
            System.out.println("[" + (char) 27 + "[34m" + "INFO" + (char) 27 + "[0m]"
                    + (tag != null ? " - " + tag.toUpperCase() : ": ") + message);
        }

    }

    @Override
    public void Debug(String message) {
        this.Debug(message, null);
    }

    @Override
    public void Debug(String message, String tag) {
        if (this.logLevel == IOTC_LOGGING.ALL) {
            System.out
                    .println("[" + (char) 27 + "[33m" + "DEBUG" + (char) 27 + "[0m]"
                    + (tag != null ? " - " + tag.toUpperCase() : ": ") + message);
        }
    }

}