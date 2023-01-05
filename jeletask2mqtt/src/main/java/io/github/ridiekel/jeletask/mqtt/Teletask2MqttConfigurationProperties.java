package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties(prefix = "teletask")
public class Teletask2MqttConfigurationProperties {

    private TeletaskConfigurationProperties central;
    private MqttConfiguration mqtt;
    private LoggingConfiguration log;
    private PublishConfiguration publish;

    public TeletaskConfigurationProperties getCentral() {
        return central;
    }

    public void setCentral(TeletaskConfigurationProperties central) {
        this.central = central;
    }

    public void setMqtt(MqttConfiguration mqtt) {
        this.mqtt = mqtt;
    }

    public MqttConfiguration getMqtt() {
        return this.mqtt;
    }

    public PublishConfiguration getPublish() {
        if (this.publish == null) {
            this.publish = new PublishConfiguration();
        }
        return publish;
    }

    public void setPublish(PublishConfiguration publish) {
        this.publish = publish;
    }

    public LoggingConfiguration getLog() {
        if (this.log == null) {
            this.log = new LoggingConfiguration();
        }
        return this.log;
    }

    public void setLog(LoggingConfiguration log) {
        this.log = log;
    }

    public static class MqttConfiguration {
        private String host;
        private String port;
        private String clientId;
        private String username;
        private String password;
        private String prefix;
        private boolean retained = false;
        private String discoveryPrefix;

        public boolean isRetained() {
            return retained;
        }

        public void setRetained(boolean retained) {
            this.retained = retained;
        }

        public String getHost() {
            return Optional.ofNullable(this.host).orElse("localhost");
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPort() {
            return Optional.ofNullable(this.port).orElse("1883");
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getClientId() {
            return Optional.ofNullable(this.clientId).orElse("teletask2mqtt");
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getDiscoveryPrefix() {
            return discoveryPrefix;
        }

        public void setDiscoveryPrefix(String discoveryPrefix) {
            this.discoveryPrefix = discoveryPrefix;
        }
    }

    public static class LoggingConfiguration {
        private boolean haconfigEnabled = false;
        private boolean topicEnabled = false;

        public boolean isHaconfigEnabled() {
            return haconfigEnabled;
        }

        public void setHaconfigEnabled(boolean haconfigEnabled) {
            this.haconfigEnabled = haconfigEnabled;
        }

        public boolean isTopicEnabled() {
            return topicEnabled;
        }

        public void setTopicEnabled(boolean topicEnabled) {
            this.topicEnabled = topicEnabled;
        }
    }

    public static class PublishConfiguration {
        private boolean motorPosition = true;
        private Integer motorPositionInterval = 250;
        private Integer statesInterval = 300;

        public boolean isMotorPosition() {
            return motorPosition;
        }

        public void setMotorPosition(boolean motorPosition) {
            this.motorPosition = motorPosition;
        }

        public Integer getMotorPositionInterval() {
            return motorPositionInterval;
        }

        public void setMotorPositionInterval(Integer motorPositionInterval) {
            this.motorPositionInterval = motorPositionInterval;
        }

        public Integer getStatesInterval() {
            return statesInterval;
        }

        public void setStatesInterval(Integer statesInterval) {
            this.statesInterval = statesInterval;
        }
    }

}
