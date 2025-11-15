package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HAConfig<T extends HAConfig<T>> {
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Getter
    private final ObjectNode config;
    private final ObjectNode device;
    private final ArrayNode deviceIdentifiers;
    @Setter
    private MQTTConfigTopic mqttConfigTopic;

    public HAConfig(HAConfigParameters parameters) {
        this.config = OBJECT_MAPPER.createObjectNode();
        this.device = this.config.putObject("device");

        ObjectNode availability = this.config.putObject("availability");
        availability.put("topic", parameters.getAvailabilityTopic());

        this.deviceIdentifiers = this.device.putArray("identifiers");

        this.baseTopic(parameters.getComponentTopic())
                .stateTopic("~/state")
                .defaultEntityId(idWithDomain(parameters))
                .uniqueId(id(parameters))
                .manufacturer("Teletask")
                .model(String.format("%s %s (%s)", parameters.getCentralUnit().getCentralUnitType().getDisplayName(), parameters.getDeviceType(), parameters.getIdentifier()));

        this.configureDevice(parameters);

        this.viaDevice(parameters.getViaDevice().map(HAConfig::getDeviceIdentifier).orElse(this.getDeviceIdentifier()));
    }

    private void configureDevice(HAConfigParameters parameters) {
        if (parameters.getConfig().getPublish().isAsOneDevice() && !parameters.getComponentSpec().equals(parameters.getCentralUnit().getBridge())) {
            this.name(parameters.getComponentSpec().getDescription())
                    .deviceIdentifier(parameters.getCentralUnit().getBridge().getHaPublishedConfig().getDeviceIdentifier())
                    .deviceName(parameters.getCentralUnit().getBridge().getHaPublishedConfig().getDeviceName());
        } else {
            this.name(null) // HA shows this name concatenated with the device name. In our case this would mean a double description. If you don't set this, HA generates a name. If you set this to null, HA just uses the device name
                    .deviceIdentifier(id(parameters))
                    .deviceName(parameters.getComponentSpec().getDescription());
        }
    }

    @JsonIgnore
    public String getDeviceIdentifier() {
        return Optional.ofNullable(this.deviceIdentifiers.get(0)).map(JsonNode::asText).orElse(null);
    }

    public T baseTopic(String value) {
        return this.put("~", value);
    }

    public T deviceIdentifier(String identifier) {
        return this.put(this.deviceIdentifiers, identifier);
    }

    public T viaDevice(String identifier) {
        return this.putDeviceProperty("via_device", identifier);
    }

    public T deviceName(String name) {
        return this.putDeviceProperty("name", name);
    }

    @JsonIgnore
    public String getDeviceName() {
        return this.getDeviceProperty("name");
    }

    public T manufacturer(String value) {
        return this.putDeviceProperty("manufacturer", value);
    }

    public T model(String value) {
        return this.putDeviceProperty("model", value);
    }

    public T defaultEntityId(String value) {
        return this.put("default_entity_id", value);
    }

    public T uniqueId(String value) {
        return this.put("unique_id", value);
    }

    public T name(String value) {
        return this.put("name", value);
    }

    public T stateTopic(String value) {
        return this.put("state_topic", value);
    }

    public T deviceClass(String value) {
        return this.put("device_class", value);
    }

    public T stateClass(String value) {
        return this.put("state_class", value);
    }

    private T putDeviceProperty(String key, String value) {
        return this.put(this.device, key, value);
    }

    private String getDeviceProperty(String key) {
        return this.get(this.device, key);
    }

    public T put(String key, String value) {
        return this.put(this.config, key, value);
    }

    private T put(ObjectNode node, String key, String value) {
        node.put(key, value);
        return this.self();
    }

    private String get(ObjectNode node, String key) {
        return node.get(key).asText();
    }

    public String getStringValue(String key) {
        return this.config.get(key).asText();
    }

    public T putBoolean(String key, boolean value) {
        return this.putBoolean(this.config, key, value);
    }

    private T putBoolean(ObjectNode node, String key, boolean value) {
        node.put(key, value);
        return this.self();
    }

    public T putDouble(String key, Double value) {
        return this.putDouble(this.config, key, value);
    }

    private T putDouble(ObjectNode node, String key, Double value) {
        node.put(key, value);
        return this.self();
    }


    public T putInt(String key, int value) {
        return this.putInt(this.config, key, value);
    }

    private T putInt(ObjectNode node, String key, int value) {
        node.put(key, value);
        return this.self();
    }

    public T putArray(String key, String... value) {
        return this.putArray(this.config, key, value);
    }

    private T putArray(ObjectNode node, String key, String... value) {
        ArrayNode array = node.putArray(key);
        Stream.of(value).forEach(array::add);
        return this.self();
    }

    private T put(ArrayNode node, String value) {
        node.add(value);
        return this.self();
    }

    protected String idWithDomain(HAConfigParameters parameters) {
        String id = id(parameters);
        //For some reason. If I don't prefix this with <something>. HA will not use this value. It doesn't even have to be correct. Just for fun I tried it with smurf, and it also worked. I just added the domain, since that is what I think it should be.
        return parameters.getDeviceType().toString().toLowerCase() + "." + id;
    }

    private static String id(HAConfigParameters parameters) {
        String id = baseId(parameters) + "-" + parameters.getComponentSpec().getFunction().toString().toLowerCase() + "-" + parameters.getComponentSpec().getNumber();
        id = removeInvalid(id, "_");
        return id;
    }

    private static String baseId(HAConfigParameters parameters) {
        return "teletask-" + parameters.getIdentifier();
    }

    private static String removeInvalid(String value, String replacement) {
        return INVALID_CHARS.matcher(value).replaceAll(replacement);
    }

    @SuppressWarnings("unchecked")
    public T self() {
        return (T) this;
    }

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this.config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        HAConfig<?> haConfig = (HAConfig<?>) o;

        return new EqualsBuilder().append(toString(), haConfig.toString()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(toString()).toHashCode();
    }

    @JsonIgnore
    public MQTTConfigTopic getMqttConfigTopic() {
        return mqttConfigTopic;
    }

    public record MQTTConfigTopic(
            String topic,
            String message
    ) {

    }
}
