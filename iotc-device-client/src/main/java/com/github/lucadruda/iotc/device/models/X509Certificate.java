package com.github.lucadruda.iotc.device.models;

public class X509Certificate {

    private String certificate;
    private String privateKey;
    private String passphrase;

    public X509Certificate(String cert, String privateKey) {
        this(cert, privateKey, null);
    }

    public X509Certificate(String certificate, String privateKey, String passphrase) {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}
