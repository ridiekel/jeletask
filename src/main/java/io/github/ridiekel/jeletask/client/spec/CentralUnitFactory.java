package io.github.ridiekel.jeletask.client.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class CentralUnitFactory {
    private static final Logger LOG = LogManager.getLogger();

    @Bean
    @Order(10)
    public CentralUnit centralUnit(Teletask2MqttConfigurationProperties configuration)  {
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //convert json string to object
        CentralUnit centralUnit;

        try (InputStream inputStream = configuration.getConfigFile().getInputStream()) {
            centralUnit = objectMapper.readValue(inputStream, CentralUnit.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String host = configuration.getCentral().getHost();
        int port = configuration.getCentral().getPort();

        centralUnit.setHost(host);
        centralUnit.setPort(port);
        centralUnit.setVersion(configuration.getCentral().getVersion());

        LOG.info("host: {}", host);
        LOG.info("port: {}", port);

        centralUnit.getAllComponents(); //Needed for function init on ComponentSpec

        LOG.debug("CentralUnit initialized.");

        return centralUnit;
    }
}
