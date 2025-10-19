package io.github.ridiekel.jeletask;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.mockserver.TeletaskMockServer;
import io.github.ridiekel.jeletask.mqtt.container.TestContainers;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;

import static com.codeborne.selenide.Configuration.*;
import static com.codeborne.selenide.Selenide.open;

@SpringBootApplication
@EnableConfigurationProperties(Teletask2MqttConfigurationProperties.class)
@EnableScheduling
@RequiredArgsConstructor
//@TestPropertySource(properties = {
//        "spring.boot.admin.client.enabled=true"
//})
//@EnableAdminServer
public class Teletask2MqttTestApplication {
    private final HomeAssistantContainer homeAssistantContainer;
    private final Teletask2MqttConfigurationProperties config;

    @Bean
    public TeletaskMockServer teletaskTestServer(CentralUnit centralUnit, TestContainers containers) {
        return new TeletaskMockServer(centralUnit.getPort(), centralUnit);
    }

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", (System.getProperties().contains("spring.profiles.active") ? System.getProperty("spring.profiles.active") + "," : "") + "test");
        Teletask2MqttApplication.handleDeprecations();
        SpringApplication.run(Teletask2MqttTestApplication.class, args);
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    public void started() {
        if (config.getTest().isStartbrowser()) {
            System.setProperty("java.awt.headless", "true");
            Configuration.browserCapabilities = new ChromeOptions()
                    .addArguments("--no-sandbox")
                    .addArguments("--lang=en_US")
                    .addArguments("--disable-blink-features=AutomationControlled");

            downloadsFolder = ".ci/selenide";
            reportsFolder = "target/frontend-reports";
            browser = "chrome";
            browserSize = "1600x1200";
            headless = false;

            Executors.newSingleThreadScheduledExecutor().execute(() -> {
                open("http://" + homeAssistantContainer.getHost() + ":" + homeAssistantContainer.getPort());
                Selenide.sleep(Long.MAX_VALUE);
            });
        }
    }
}
