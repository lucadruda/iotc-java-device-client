package com.github.lucadruda.iotc.device.models;

public class RegistrationResult {
    private String assignedHub;
    private String status;
    private RegistrationResult registrationState;

    public String getStatus() {
        return status;
    }

    public RegistrationResult getRegistrationState() {
        return registrationState;
    }

    public void setRegistrationState(RegistrationResult registrationState) {
        this.registrationState = registrationState;
    }

    public String getAssignedHub() {
        return assignedHub;
    }

    public void setAssignedHub(String assignedHub) {
        this.assignedHub = assignedHub;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
