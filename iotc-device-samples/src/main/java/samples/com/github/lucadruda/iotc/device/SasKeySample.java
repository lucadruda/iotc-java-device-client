package samples.com.github.lucadruda.iotc.device;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.github.lucadruda.iotc.device.ICentralStorage;
import com.github.lucadruda.iotc.device.IoTCClient;
import com.github.lucadruda.iotc.device.callbacks.CommandCallback;
import com.github.lucadruda.iotc.device.callbacks.PropertiesCallback;
import com.github.lucadruda.iotc.device.enums.IOTC_COMMAND_RESPONSE;
import com.github.lucadruda.iotc.device.enums.IOTC_CONNECT;
import com.github.lucadruda.iotc.device.enums.IOTC_EVENTS;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;
import com.github.lucadruda.iotc.device.models.IoTCProperty;
import com.github.lucadruda.iotc.device.models.Storage;

public class SasKeySample {

    final static String TELEMETRY_COMPONENT = "firstComponent";
    final static String TELEMETRY_FIELD = "temperature";

    static class MemStorage implements ICentralStorage {

        @Override
        public void persist(Storage storage) {
            System.out.println("New credentials available:");
            System.out.println(storage.getHubName());
            System.out.println(storage.getDeviceId());
            System.out.println(new String(storage.getDeviceKey(), StandardCharsets.UTF_8));
            return;
        }

        @Override
        public Storage retrieve() {
            // return new
            // Storage("iotc-1f9e162c-eacc-408d-9fb2-c9926a071037.azure-devices.net",
            // "javasdkcomponents",
            // "+yz0YcYq/SEwvaF0UjLNKyrKuL8oyFknTtoEJOfOgTo=".getBytes());
            return new Storage();
        }

    }

    public static void main(String[] args) {
        System.out.println("Welcome to IoTCentral");
        Properties config = new Properties();
        try {
            FileInputStream in = new FileInputStream(System.getProperty("user.dir") + "/samples.config");
            config.load(in);
            in.close();
        } catch (IOException e) {
            System.out.println("Error parsing configuration file 'samples.config'");
            System.exit(1);
        }
        String deviceId = config.getProperty("deviceId");
        String scopeId = config.getProperty("scopeId");
        String groupKey = config.getProperty("groupKey");
        String deviceKey = config.getProperty("deviceKey");
        String modelId = config.getProperty("modelId");

        IoTCClient client;
        if (groupKey != null && deviceKey == null) {
            client = new IoTCClient(deviceId, scopeId, IOTC_CONNECT.SYMM_KEY, groupKey, new MemStorage());
        } else {
            client = new IoTCClient(deviceId, scopeId, IOTC_CONNECT.DEVICE_KEY, deviceKey, new MemStorage());
        }
        if (modelId != null) {
            client.SetModelId(modelId);
        }

        PropertiesCallback onProps = (IoTCProperty property) -> {
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
            client.SendProperty(String.format("{\"propertyComponent\":{\"__t\":\"c\",\"prop1\":%d}}", 20));

            while (true) {
                System.out.println("Sending telemetry");
                client.SendTelemetry(String.format("{\"%s\":%,.0f}", TELEMETRY_FIELD, Math.random() * 30),
                        String.format("{\"$.sub\":\"%s\"}", TELEMETRY_COMPONENT));
                Thread.sleep(4000);
            }

        } catch (IoTCentralException | InterruptedException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }
}
