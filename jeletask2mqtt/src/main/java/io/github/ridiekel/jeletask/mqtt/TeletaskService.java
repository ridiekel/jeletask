package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.config.model.json.JsonCentralUnit;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class TeletaskService {
    private static final Logger LOG = LoggerFactory.getLogger(TeletaskService.class);

    private TeletaskClient client;

    private final Teletask2MqttConfiguration configuration;

    public TeletaskService(Teletask2MqttConfiguration configuration) {
        this.configuration = configuration;
    }

    public TeletaskClient createClient(MqttProcessor processor) {
        if (this.client == null) {
            CentralUnit centralUnit = this.createCentralUnit();

            LOG.info("Creating Teletask client...");
            this.client = new TeletaskClientImpl(centralUnit);

            LOG.info("Registering MQTT processor...");
            this.client.registerStateChangeListener(processor);

            LOG.info("Starting Teletask client...");
            this.client.start();
        }

        return this.client;
    }

    public Teletask2MqttConfiguration getConfiguration() {
        return this.configuration;
    }

    private CentralUnit createCentralUnit() {
        try (InputStream inputStream = Files.newInputStream(Paths.get(this.configuration.getConfigFile()))) {
            JsonCentralUnit read = JsonCentralUnit.read(inputStream);

            String host = this.getConfiguration().getHost();
            int port = this.getConfiguration().getPort();

            read.setHost(host);
            read.setPort(port);

            LOG.info("host: {}", host);
            LOG.info("port: {}", port);

            return read;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
