package io.github.ridiekel.jeletask.client.spec;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.config.CentralUnitConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Configuration
public class CentralUnitFactory {
    private static final Logger LOG = LogManager.getLogger();

    @Bean
    @Order(10)
    public CentralUnit centralUnit(Teletask2MqttConfigurationProperties configuration)  {
        Resource configFile = configuration.getConfigFile();
        CentralUnitConfigReader.Result config;
        try (InputStream inputStream = configFile.getInputStream()) {
            config = CentralUnitConfigReader.read(inputStream, configFile.getDescription());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the config file " + configFile.getDescription(), e);
        }
        config.warnings().forEach(warning -> LOG.warn("Config file: {}", warning));
        CentralUnit centralUnit = config.centralUnit();
        String host = configuration.getCentral().getHost();
        int port = configuration.getCentral().getPort();

        centralUnit.setHost(host);
        centralUnit.setPort(port);
        centralUnit.setVersion(configuration.getCentral().getVersion());

        LOG.info("host: {}", host);
        LOG.info("port: {}", port);

        centralUnit.getAllComponents(); //Adds the bridge and the devices

        LOG.debug("CentralUnit initialized.");

        return centralUnit;
    }
}
