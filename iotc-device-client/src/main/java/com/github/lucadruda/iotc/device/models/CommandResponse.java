package com.github.lucadruda.iotc.device.models;

import com.github.lucadruda.iotc.device.enums.IOTC_COMMAND_RESPONSE;

@FunctionalInterface
public interface CommandResponse {
    IOTC_COMMAND_RESPONSE sendResponse(String commandName, Object value);
}
