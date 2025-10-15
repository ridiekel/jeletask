package io.github.ridiekel.jeletask;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;

import static com.codeborne.selenide.Configuration.*;

public abstract class TeletaskTestSupport {
    @Autowired
    private HomeAssistantContainer ha;
    @Autowired
    private MqttContainer mqtt;
    @Autowired
    private Teletask2MqttConfigurationProperties config;

    @BeforeEach
    public void configure() {
        this.mqtt().reset();
        System.setProperty("java.awt.headless", "true");
        Configuration.browserCapabilities = new ChromeOptions()
                .addArguments("--no-sandbox")
                .addArguments("--lang=en_US")
                .addArguments("--disable-blink-features=AutomationControlled");

        downloadsFolder = ".ci/selenide";
        reportsFolder = "target/frontend-reports";
        timeout = config.getTest().getTimeout();
        browser = "chrome";
        browserSize = "1600x1200";
        headless = config.getTest().isHeadless();
    }

    protected void keepRunning() {
        if (config.getTest().isKeeprunning()) {
            System.out.println("****************************** KEEP RUNNING");
            Selenide.sleep(Long.MAX_VALUE);
        }
    }

    protected HomeAssistantContainer ha() {
        return ha;
    }

    protected MqttContainer mqtt() {
        return mqtt;
    }
}

