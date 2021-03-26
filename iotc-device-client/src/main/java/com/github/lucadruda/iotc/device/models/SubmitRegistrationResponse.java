package com.github.lucadruda.iotc.device.models;

public class SubmitRegistrationResponse {
    private String status;
    private String operationId;
    private RegistrationResult registrationState;

    public RegistrationResult getRegistrationState() {
        return registrationState;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public void setRegistrationState(RegistrationResult registrationState) {
        this.registrationState = registrationState;
    }
}
