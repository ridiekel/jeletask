package io.github.ridiekel.jeletask.sba.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitFactory;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest(classes = {CentralUnitEndpoint.class, CentralUnitFactory.class, CentralUnitEndpointTest.TestBeans.class})
class CentralUnitEndpointTest {
    @Autowired
    private CentralUnitEndpoint endpoint;
    @Autowired
    private CentralUnit centralUnit;

    @Test
    void test() throws JsonProcessingException {
        centralUnit.getAllComponents().forEach(c->{
            c.setState(new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
        });

        System.out.println(endpoint.getCentralUnitInfo());
    }

    @Configuration
    public static class TestBeans {
        @Bean
        public @NotNull Teletask2MqttConfigurationProperties teletask2MqttConfigurationProperties() {
            Teletask2MqttConfigurationProperties configuration = new Teletask2MqttConfigurationProperties();
            configuration.setConfigFile(new ClassPathResource("/test-config.json"));
            configuration.setCentral(new Teletask2MqttConfigurationProperties.CentralConfiguration());
            return configuration;
        }

    }

}