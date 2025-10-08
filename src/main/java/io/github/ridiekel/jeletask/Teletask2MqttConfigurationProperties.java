package io.github.ridiekel.jeletask;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.Optional;

@ConfigurationProperties(prefix = "teletask")
public class Teletask2MqttConfigurationProperties {
    private CentralConfiguration central;
    private MqttConfiguration mqtt;
    private LoggingConfiguration log;
    private PublishConfiguration publish;
    private TestConfiguration test;
    private Resource configFile;

    public TestConfiguration getTest() {
        return test;
    }

    public void setTest(TestConfiguration test) {
        this.test = test;
    }

    public Resource getConfigFile() {
        return this.configFile;
    }

    public void setConfigFile(Resource configFile) {
        this.configFile = configFile;
    }

    public CentralConfiguration getCentral() {
        return central;
    }

    public void setCentral(CentralConfiguration central) {
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

    public static class CentralConfiguration {
        private String version;
        private String host;
        private String id;
        private int port;

        public CentralConfiguration() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getHost() {
            return this.host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return this.port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class TestConfiguration {
        private boolean headless = true;
        private Integer timeout = 60000;
        private boolean gitlabci = false;
        private boolean keeprunning = false;

        public boolean isKeeprunning() {
            return keeprunning;
        }

        public void setKeeprunning(boolean keeprunning) {
            this.keeprunning = keeprunning;
        }

        public boolean isGitlabci() {
            return gitlabci;
        }

        public void setGitlabci(boolean gitlabci) {
            this.gitlabci = gitlabci;
        }

        public boolean isHeadless() {
            return headless;
        }

        public void setHeadless(boolean headless) {
            this.headless = headless;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

}
