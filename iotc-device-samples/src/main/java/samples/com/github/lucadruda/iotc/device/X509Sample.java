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

    static final String deviceId = "";
    static final String scopeId = "";
    static final String cert = "";

    static final String privateKey="";


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
            return new Storage();
        }

    }

    public static void main(String[] args) {
        System.out.println("Welcome to IoTCentral");
        IoTCClient client = new IoTCClient(deviceId, scopeId, IOTC_CONNECT.X509_CERT, new X509Certificate(cert, privateKey), new MemStorage());
        client.SetLogging(IOTC_LOGGING.ALL);

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
