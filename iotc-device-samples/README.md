# Samples for the Azure IoT Central device SDK for Java
This folder contains simple samples showing how to use the various features of the Microsoft Azure IoT Hub service from a device running Java code.

## List of samples
- [Send/receive sample with SAS key authentication:](./src/main/java/samples/com/github/lucadruda/iotc/device/SasKeySample.java) Shows how to connect, send telemetry and properties, receive properties and commands and manage credentials cache when authenticating through SAS key (either individual or group key).
- [Send/receive sample with X509 authentication:](./src/main/java/samples/com/github/lucadruda/iotc/device/X509Sample.java): Shows how to connect, send telemetry and properties, receive properties and commands and manage credentials cache when authenticating through X509 certificates.

## How to run samples

### Build
Navigate to the sample folder and build if not already done from root folder:
```sh
mvn compile
```

### Create configuration file
Create a file named **"samples.config"** in the sample folder ("iotc-device-samples") close to _pom.xml_.
Put entries in _"key=value"_ format one for each line.
Property keys are case sensitive and not all required.

Supported properties:
- deviceId (required)
- scopeId (required)
- groupKey (either this or _deviceKey_)
- deviceKey (either this or _groupKey_)
- modelId
- cert
- privateKey

### Run

From samples folder run following commands depending on the sample to run:
- SASKey:
```sh
mvn exec:exec -P saskeysample
```
- X509:
```sh
mvn exec:exec -P certsample
```
