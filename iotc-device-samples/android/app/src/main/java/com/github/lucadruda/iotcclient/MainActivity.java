package com.github.lucadruda.iotcclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.lucadruda.iotc.device.ILogger;
import com.github.lucadruda.iotc.device.IoTCClient;
import com.github.lucadruda.iotc.device.callbacks.CommandCallback;
import com.github.lucadruda.iotc.device.callbacks.PropertiesCallback;
import com.github.lucadruda.iotc.device.enums.IOTC_COMMAND_RESPONSE;
import com.github.lucadruda.iotc.device.enums.IOTC_CONNECT;
import com.github.lucadruda.iotc.device.enums.IOTC_EVENTS;
import com.github.lucadruda.iotc.device.enums.IOTC_LOGGING;
import com.github.lucadruda.iotc.device.exceptions.IoTCentralException;
import com.github.lucadruda.iotc.device.models.IoTCProperty;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    IoTCClient client;
    AppLogger logger;

    class AppLogger implements ILogger {
        private TextView _logView;

        public AppLogger(TextView logView) {
            _logView = logView;
        }


        @Override
        public void Log(String message) {
            runOnUiThread(() -> _logView.append(message + "\n"));
        }

        @Override
        public void Log(String message, String tag) {
            Log(message);

        }

        @Override
        public void Debug(String message) {
            Log(message);

        }

        @Override
        public void Debug(String message, String tag) {
            Log(message);
        }

        @Override
        public void SetLevel(IOTC_LOGGING level) {
// no-op
        }
    }

    PropertiesCallback onProps = (IoTCProperty property) -> {
        logger.Log(String.format("Received property '%s' with value: %s", property.getName(),
                property.getValue().toString()));
        property.ack("Property applied");
    };

    CommandCallback onCommand = (command) -> {
        logger.Log(String.format("Received command '%s' with value: %s", command.getName(),
                command.getRequestPayload().toString()));
        return command.reply(IOTC_COMMAND_RESPONSE.SUCCESS, "Command executed");
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button connectButton = this.findViewById(R.id.connectBtn);
        TextView logView = this.findViewById(R.id.logsView);
        EditText deviceId = this.findViewById(R.id.deviceIdField);
        EditText scopeId = this.findViewById(R.id.scopeIdField);
        EditText deviceKey = this.findViewById(R.id.deviceKeyField);
        ProgressBar progress = this.findViewById(R.id.progressBar);
        Thread main = new Thread() {
            private AtomicBoolean stop = new AtomicBoolean(false);

            public void run() {
                try {
                    runOnUiThread(() -> progress.setVisibility(View.VISIBLE));
                    client.Connect();
                    runOnUiThread(() -> {
                        connectButton.setText("Disconnect");
                        connectButton.setEnabled(true);
                        progress.setVisibility(View.INVISIBLE);
                    });
                    client.SendProperty(String.format("{\"fanSpeed\":%d}", 20));

                    while (!stop.get() && client.IsConnected()) {
                        client.SendTelemetry(String.format("{\"temperature\":%,.0f}", Math.random() * 30));
                        Thread.sleep(4000);
                    }
                } catch (InterruptedException | IoTCentralException ex) {
                    System.out.println("Exception: " + ex.getMessage());
                }
            }

            public void interrupt() {
                stop.set(true);
            }


        };
        connectButton.setOnClickListener(view -> {
            if (connectButton.getText().toString() == "Disconnect") {
                try {
                    client.Disconnect();
                    client = null;
                    main.interrupt();
                    runOnUiThread(() -> {
                        connectButton.setText("Connect");
                        logView.setText("");
                    });
                } catch (IoTCentralException e) {
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(() -> connectButton.setEnabled(false));
                logger = new AppLogger(logView);
                client = new IoTCClient(deviceId.getText().toString(), scopeId.getText().toString(), IOTC_CONNECT.DEVICE_KEY, deviceKey.getText().toString(), new MemoryStorage(), logger);
                /*client.SetProtocol(IOTC_PROTOCOL.HTTPS);*/
                client.on(IOTC_EVENTS.Properties, onProps);
                client.on(IOTC_EVENTS.Commands, onCommand);

                main.start();
            }
        });
    }
}