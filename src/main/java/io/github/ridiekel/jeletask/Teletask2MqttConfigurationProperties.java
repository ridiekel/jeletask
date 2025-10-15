package io.github.ridiekel.jeletask;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.Optional;

@Setter
@ConfigurationProperties(prefix = "teletask")
public class Teletask2MqttConfigurationProperties {
    @Getter
    private CentralConfiguration central;
    @Getter
    private MqttConfiguration mqtt;
    private LoggingConfiguration log;
    private PublishConfiguration publish;
    @Getter
    private TestConfiguration test;
    @Getter
    private Resource configFile;

    public PublishConfiguration getPublish() {
        if (this.publish == null) {
            this.publish = new PublishConfiguration();
        }
        return publish;
    }

    public LoggingConfiguration getLog() {
        if (this.log == null) {
            this.log = new LoggingConfiguration();
        }
        return this.log;
    }

    @Setter
    public static class MqttConfiguration {
        private String host;
        private String port;
        private String clientId;
        @Getter
        private String username;
        @Getter
        private String password;
        @Getter
        private String prefix;
        @Getter
        private boolean retained = false;
        @Getter
        private String discoveryPrefix;

        public String getHost() {
            return Optional.ofNullable(this.host).orElse("localhost");
        }

        public String getPort() {
            return Optional.ofNullable(this.port).orElse("1883");
        }

        public String getClientId() {
            return Optional.ofNullable(this.clientId).orElse("teletask2mqtt");
        }

    }

    @Setter
    @Getter
    public static class LoggingConfiguration {
        private boolean haconfigEnabled = false;
        private boolean topicEnabled = false;

    }

    @Setter
    @Getter
    public static class PublishConfiguration {
        private boolean motorPosition = true;
        private Integer motorPositionInterval = 250;
        private Integer statesInterval = 300;

    }

    @Setter
    @Getter
    public static class CentralConfiguration {
        private String version;
        private String host;
        private String id;
        private int port;

        public CentralConfiguration() {
        }

    }

    @Setter
    @Getter
    public static class TestConfiguration {
        private boolean headless = true;
        private Integer timeout = 60000;
        private boolean gitlabci = false;
        private boolean keeprunning = false;
        private boolean startbrowser = false;
    }
}
