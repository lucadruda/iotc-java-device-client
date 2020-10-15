package samples.com.github.lucadruda.iotc.device;

import com.github.lucadruda.iotc.device.ICentralStorage;
import com.github.lucadruda.iotc.device.IoTCClient;
import com.github.lucadruda.iotc.device.callbacks.CommandCallback;
import com.github.lucadruda.iotc.device.callbacks.PropertiesCallback;
import com.github.lucadruda.iotc.device.enums.IOTC_COMMAND_RESPONSE;
import com.github.lucadruda.iotc.device.enums.IOTC_CONNECT;
import com.github.lucadruda.iotc.device.enums.IOTC_EVENTS;
import com.github.lucadruda.iotc.device.enums.IOTC_LOGGING;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;
import com.github.lucadruda.iotc.device.models.Storage;
import com.github.lucadruda.iotc.device.models.X509Certificate;

public class X509Sample {

    static final String deviceId = "java";
    static final String scopeId = "0ne0011423C";
    static final String cert = "-----BEGIN CERTIFICATE-----\n"
            + "MIIF0zCCA7ugAwIBAgICBREwDQYJKoZIhvcNAQELBQAwcjELMAkGA1UEBhMCVVMx\n"
            + "EzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxDjAMBgNVBAoT\n"
            + "BUF6dXJlMRIwEAYDVQQLEwlBenVyZSBJb1QxGDAWBgNVBAMTD0F6dXJlSW9UQ2Vu\n"
            + "dHJhbDAeFw0yMDEwMTUxNDAxNTNaFw0yMTEwMTUxNDAxNTNaMGcxCzAJBgNVBAYT\n"
            + "AlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMQ4wDAYD\n"
            + "VQQKEwVBenVyZTESMBAGA1UECxMJQXp1cmUgSW9UMQ0wCwYDVQQDEwRqYXZhMIIC\n"
            + "IjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAzk1Q5ZLIroXeOdWsJ0Gre0G1\n"
            + "NM1ZSj/xlDpFo2DKVIaduVW/ark+dDOTvuEGnxcobSgPssL6inrXORcROxXBZcL3\n"
            + "3Uv5d0zPZUbPArMxCE6wdupwYMavA6AsPXfrJAktznws1zgcwFnL6nC6ajMOivkb\n"
            + "dTZgnUi/ymgL3+gC3xomONJw/pqxTLOWnDBBNnKQToGtNIm4q2a5tBbDKTlPQs5D\n"
            + "xdHRyFWkULw6X9QQ+BUQJhX0x/CdvpDLiW0aO1ubAfbgGP8Ifk7Xx+3dY+5ukYlG\n"
            + "IanLhNmd6XLcOmyt/rmwSIUfsjSRJtD6GTI5WIe+2g3wM46ih5PEeHZ9cYkMAsu7\n"
            + "ZuoXIZMCsnvM8SqiTSVqqrxoM7sslziMsEFWcrCwAWFn4aGpeG2kovEbhqaLiEiw\n"
            + "KdcwfMgsTYeV//jdxZkm6vptm6meRiOXz8oFTEPkhtrLm8UztFdE7lg1mcfWJlML\n"
            + "myyZwBR8ou1k+aVsdmChu6OjU4k/TG6nhW1hJA8LtuZmBcONgBsfjezXSKX0/rIG\n"
            + "fQEdnQ9nv/QM9/fHVhIezQBfgMaq3/su9PX7IyWmeu8H0XogExKnDfjvFwVavVxt\n"
            + "DptM2JZyDkuuMOlQs8nWe0jd3OChsiJQ9lY7AnR+OY4gsAd3kaLeW6lrQ1ncCLdZ\n"
            + "YXBZg7MVzx7ZP4pvz80CAwEAAaN+MHwwHQYDVR0OBBYEFOjqGpmosuPwioG8QMBP\n"
            + "7rAvwgFmMAsGA1UdDwQEAwIC9DA7BgNVHSUENDAyBggrBgEFBQcDAQYIKwYBBQUH\n"
            + "AwIGCCsGAQUFBwMDBggrBgEFBQcDBAYIKwYBBQUHAwgwEQYJYIZIAYb4QgEBBAQD\n"
            + "AgD3MA0GCSqGSIb3DQEBCwUAA4ICAQCiWLXxT12iFrEmyV9EXiPzPjZWD1CIuXWD\n"
            + "CHirRa7hvn5+kwuMaY4nhhwEb/7tMT+D63wW00l2XPiOEzmZwWRAap/B33eRkj1i\n"
            + "SLhnVMfQbOQdFPT8pAxRun9jyeIKKtULJExPsVJ0rQ3dljdXoV547sHnHVzKrYNq\n"
            + "E+ty+VXI5yB3x2TyN2qfAClBNSaMaNM8yVauTt0qpTueAt46gkcnTi/FyEalY/Tl\n"
            + "klRFjc0OwF/AvfIBSsY5NcTVJIy9rVzg5uB3Rqzn+csh+90dCeTWgb0CtewIgQFZ\n"
            + "LO1DzaNLt/F7C8AlU9+HjfCcP1sKNrekO0nPFxXIC7iyo2knenpFVma+DLfXfmtr\n"
            + "h7eRjiO9TDE5b/Rw44zpkwgrEk9icHrNBzspabOeJ/3CDioz0p6K5SGjvv4sGxeZ\n"
            + "dEgyIll1PcIrdYJNe1bf7TQVCneiCo0RJZdLgQN54wGu/bzZAhcqwen9yrDZTnow\n"
            + "VpJC2qskKfuRi7R+0z+JSGElrLWiIqUlPDE60x6AfQI98OUevsWdKnjB/sfxCZd8\n"
            + "TWIkkncfgu/u/xwqECD9DNaBxeJXQht2O4laqQpHJ8kCj0di5FuXeTGUYAamK1vS\n"
            + "ubsWrY24tDMCjvZPdxQyUJ/tEQmN9MJFvIHOlDaWTAtkm2oZ/vHF0dRmh0dHJ5Sf\n" + "JL7c60aJ2w==\n"
            + "-----END CERTIFICATE-----\n" + "-----BEGIN CERTIFICATE-----\n"
            + "MIIF7jCCA9agAwIBAgICBQYwDQYJKoZIhvcNAQELBQAwcjELMAkGA1UEBhMCVVMx\n"
            + "EzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxDjAMBgNVBAoT\n"
            + "BUF6dXJlMRIwEAYDVQQLEwlBenVyZSBJb1QxGDAWBgNVBAMTD0F6dXJlSW9UQ2Vu\n"
            + "dHJhbDAeFw0yMDEwMTUxNDAxNTJaFw0yMTEwMTUxNDAxNTJaMHIxCzAJBgNVBAYT\n"
            + "AlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMQ4wDAYD\n"
            + "VQQKEwVBenVyZTESMBAGA1UECxMJQXp1cmUgSW9UMRgwFgYDVQQDEw9BenVyZUlv\n"
            + "VENlbnRyYWwwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDQlAxLkPbT\n"
            + "BAwX3BYZZTLb1r8uhJ9A0WP5fKriW+veHq43CeiZhhuh2olTVGIutlsrzo5mquLu\n"
            + "xmhEjFoHqUI+H+yS5JId8QLEON267EtxvXaF6S59WKfDkJoj2UnlAj+EiMRt6V9u\n"
            + "W1unwDGTMh8PYSF7YpdMd/++bFzuqTGxpV3+QqESitwk15M93NvQcoFvnbcN9h43\n"
            + "lt7czoqB9stIODRTrZxfbq6VOdI2O+3N2nEmFV2u/1yGQfFZGMAXMCxjNxd1x3F8\n"
            + "ZwwRGigCOxy+BJPtXpcYLl8kwr0mUPR0O0HsMQq74a8YSvtEre0WFjHDi9KQAcCN\n"
            + "MjiLTZeg09eimpd1nDVjTR9WIxc+nmacNpbuQQOk0LkUiqmGT6HcSfxJngcXEIS3\n"
            + "j6uY6PF96MDlAhcP/iVRdtByby2PytQ8AEWVC0WOlJqM4O/338B7VzBW03eNST2U\n"
            + "m6+bVCSSDmFE3UPCGEhZ7OJi3PAtVXnjX2JWuBCvz3dK8Rr2ViKRPhN2Hm8x3063\n"
            + "CHu0dQIU+3z7mF1KDjkgyCXjWYc5AOitw5TsqF+Djt/CIDnPaQzF+z53BVcm57Xe\n"
            + "I7Zx8652Mj4kZL/+5umIuBYkwUHDRO/K+Y2jJsox7s0D+uFemuLSXTRNlMOgX3do\n"
            + "heOlVnXThDFNseMRri+bmrBry1BfeIcCMwIDAQABo4GNMIGKMB0GA1UdDgQWBBTo\n"
            + "6hqZqLLj8IqBvEDAT+6wL8IBZjALBgNVHQ8EBAMCAvQwOwYDVR0lBDQwMgYIKwYB\n"
            + "BQUHAwEGCCsGAQUFBwMCBggrBgEFBQcDAwYIKwYBBQUHAwQGCCsGAQUFBwMIMBEG\n"
            + "CWCGSAGG+EIBAQQEAwIA9zAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IC\n"
            + "AQCE9/j9aYDtH7DPdixLhYN55jhVDvarH4PzfWyWiZqtEZjuuzRiVotBS/wB+7qn\n"
            + "Sk63dzlj2ly1V5JR12FNX/kB2TBo/9jOzb/2VHiluJWlrg0WKHwQu9SdVIyDvoxn\n"
            + "oBBWsXSFxYXkQlGo3TXNgLT/8yV9pVimK05/bVdp3mrL2SGeoeG2ioipjL94O4Vd\n"
            + "GiFCW/zSGNPAieYnj3PBFbdim3xhB5aTNpoo5V5LGYyo6cfApbzIroq/FCS56n6e\n"
            + "Gt93MVYjcceN8pYT2IHuKRDhijQ/I8pyBdB+3L+xc9CnX8Ed9KLP75YcVucPFE8R\n"
            + "SQFet42wxs45mT0xoUrdEDzjGjbZboq7uWCJvKUrhNcPwHz9TReBLG2XQrGZRz4D\n"
            + "qFhBKPXgJpWdfRqQZ1c8su/1VGJNO41S3koam96zK8HuuMngtfhytiusFz6V6kId\n"
            + "bPrvMpNHB/ngERDpklFX8A9SoAaroIBRm8peKmpxAxzsGijvBCZksQdwssZK0OVa\n"
            + "S2f6K40hFzcEdWoLanzc3Aa2/wH5GQ+amN56DzBWMonxljdWWjFq67r0lc6Cw6Md\n"
            + "Y8TM+DSS42BZROYu/FWhIdPl+oVudqFVA9iTQVmGQxLZhj8vovOeYR88t9JfrpaF\n"
            + "I6f9kSWenZ/Dx5FgT2XFXLa2ivpGsGtoe3AXSX38jR5Nwg==\n" + "-----END CERTIFICATE-----";

    static final String privateKey="-----BEGIN RSA PRIVATE KEY-----\n"
+"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\n"
+"-----END RSA PRIVATE KEY-----";


    static class MemStorage implements ICentralStorage {

        @Override
        public void persist(Storage storage) {
            System.out.println("New credentials available:");
            System.out.println(storage.getHubName());
            System.out.println(storage.getDeviceId());
            return;
        }

        @Override
        public Storage retrieve() {
            return new Storage("iotc-1f9e162c-eacc-408d-9fb2-c9926a071037.azure-devices.net", "java2", "safasf");
        }

    }

    public static void main(String[] args) {
        System.out.println("Welcome to IoTCentral");
        IoTCClient client = new IoTCClient(deviceId, scopeId, IOTC_CONNECT.X509_CERT, new X509Certificate(cert, privateKey), new MemStorage());
        client.SetLogging(IOTC_LOGGING.ALL);
        // Path path = Paths.get("assets/modelId.jpg");
        // File file = path.toFile();
        PropertiesCallback onProps = (property) -> {
            System.out.println(String.format("Received property '%s' with value: %s", property.getName(),
                    property.getValue().toString()));
            property.ack("Property applied");
        };

        CommandCallback onCommand = (command) -> {
            System.out.println(String.format("Received command '%s' with value: %s", command.getName(),
                    command.getRequestPayload().toString()));
            return command.reply(IOTC_COMMAND_RESPONSE.SUCCESS, "Command executed");
        };

        client.on(IOTC_EVENTS.Properties, onProps);
        client.on(IOTC_EVENTS.Commands, onCommand);
        try {
            client.Connect();
            client.SendProperty(String.format("{\"readOnlyProp\":%d}", 20));
            // client.UploadFile(file.getName(), file);

            while (true) {
                System.out.println("Sending telemetry");
                client.SendTelemetry(String.format("{\"temperature\":%,.0f}", Math.random() * 30));
                Thread.sleep(4000);
            }

        } catch (IoTCentralException | InterruptedException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }
}
