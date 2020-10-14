package samples.com.github.lucadruda.iotc.device;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.lucadruda.iotc.device.IoTCClient;
import com.github.lucadruda.iotc.device.callbacks.CommandCallback;
import com.github.lucadruda.iotc.device.callbacks.PropertiesCallback;
import com.github.lucadruda.iotc.device.enums.IOTC_COMMAND_RESPONSE;
import com.github.lucadruda.iotc.device.enums.IOTC_CONNECT;
import com.github.lucadruda.iotc.device.enums.IOTC_EVENTS;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;

public class SasKeySample {

    static final String deviceId = "java";
    static final String scopeId = "0ne0011423C";
    static final String masterKey = "r0mxLzPr9gg5DfsaxVhOwKK2+8jEHNclmCeb9iACAyb2A7yHPDrB2/+PTmwnTAetvI6oQkwarWHxYbkIVLybEg==";

    public static void main(String[] args) {
        IoTCClient client = new IoTCClient(deviceId, scopeId, IOTC_CONNECT.SYMM_KEY, masterKey);
        client.SetModelId("urn:testapplucaM3:TestSDK_18x:2");

        Path path = Paths.get("assets/modelId.jpg");
        File file = path.toFile();
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
            client.UploadFile(file.getName(), file);

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
