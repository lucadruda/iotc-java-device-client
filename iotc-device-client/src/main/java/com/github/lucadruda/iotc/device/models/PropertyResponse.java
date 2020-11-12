package com.github.lucadruda.iotc.device.models;

@FunctionalInterface
public interface PropertyResponse {
    void sendResponse(String value);
}
