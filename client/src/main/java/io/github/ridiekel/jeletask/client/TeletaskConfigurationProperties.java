package io.github.ridiekel.jeletask.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "teletask.central")
public class TeletaskConfigurationProperties {
    private Resource configFile;
    private String version;
    private String host;
    private String id;
    private int port;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Resource getConfigFile() {
        return this.configFile;
    }

    public void setConfigFile(Resource configFile) {
        this.configFile = configFile;
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
