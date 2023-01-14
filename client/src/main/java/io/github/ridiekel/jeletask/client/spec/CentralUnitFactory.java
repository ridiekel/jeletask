package io.github.ridiekel.jeletask.client.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ridiekel.jeletask.client.TeletaskConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableConfigurationProperties(TeletaskConfigurationProperties.class)
public class CentralUnitFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CentralUnitFactory.class);

    @Bean
    @Order(10)
    public CentralUnit centralUnit(TeletaskConfigurationProperties configuration)  {
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //convert json string to object
        CentralUnit centralUnit;

        try (InputStream inputStream = configuration.getConfigFile().getInputStream()) {
            centralUnit = objectMapper.readValue(inputStream, CentralUnit.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String host = configuration.getHost();
        int port = configuration.getPort();

        centralUnit.setHost(host);
        centralUnit.setPort(port);

        LOG.info("host: {}", host);
        LOG.info("port: {}", port);

        centralUnit.getAllComponents(); //Needed for function init on ComponentSpec

        LOG.debug("CentralUnit initialized.");

        return centralUnit;
    }
}
