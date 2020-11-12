package com.github.lucadruda.iotc.device.models;

public class Storage {
    private String hubName;
    private String deviceId;
    private String deviceKey;
    private X509Certificate certificate;

    public String getHubName() {
        return hubName;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setHubName(String hubName) {
        this.hubName = hubName;
    }

    public Storage() {
    }

    public String getConnectionString() {
        if (deviceKey != null && !deviceKey.isEmpty() && hubName != null && !hubName.isEmpty() && deviceId != null
                && !deviceId.isEmpty()) {
            return String.format("HostName=%s;DeviceId=%s;SharedAccessKey=%s", hubName, deviceId, deviceKey);
        } else if (certificate != null && hubName != null && !hubName.isEmpty() && deviceId != null) {
            return String.format("HostName=%s;DeviceId=%s;x509=true", hubName, deviceId);
        }
        return null;
    }

    public Storage(String hubName, String deviceId, String deviceKey) {
        this(hubName, deviceId, deviceKey, null);
    }

    public Storage(String hubName, String deviceId, X509Certificate certificate) {
        this(hubName, deviceId, null, certificate);
    }

    public Storage(String hubName, String deviceId, String deviceKey, X509Certificate certificate) {
        this.hubName = hubName;
        this.deviceId = deviceId;
        this.deviceKey = deviceKey;
        this.certificate = certificate;
    }
}
