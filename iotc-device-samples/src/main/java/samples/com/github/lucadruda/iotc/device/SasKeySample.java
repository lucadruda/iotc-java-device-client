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

public class SasKeySample {

    static final String deviceId = "java2";
    static final String scopeId = "0ne0011423C";
    static final String deviceKey = "HYxMG7lgbchS2e/qJ/zJkTTPBCLVWbsY7/im5pdHXJc=";

    static class MemStorage implements ICentralStorage {

        @Override
        public void persist(Storage storage) {
            System.out.println("New credentials available:");
            System.out.println(storage.getHubName());
            System.out.println(storage.getDeviceId());
            System.out.println(storage.getDeviceKey());
            return;
        }

        @Override
        public Storage retrieve() {
            return new Storage("iotc-1f9e162c-eacc-408d-9fb2-c9926a071037.azure-devices.net", "java2",
                    "safasf");
        }

    }

    public static void main(String[] args) {
        System.out.println("Welcome to IoTCentral");
        IoTCClient client = new IoTCClient(deviceId, scopeId, IOTC_CONNECT.DEVICE_KEY, deviceKey, new MemStorage());
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
