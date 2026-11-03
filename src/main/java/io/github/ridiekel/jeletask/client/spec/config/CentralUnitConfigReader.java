package io.github.ridiekel.jeletask.client.spec.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.component.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reads the config file (the TDS config JSON) into a {@link CentralUnit}, with a {@link ComponentSpec} subclass per function and per sensor type.
 * <p>
 * Every problem is collected before giving up, so the user can fix the whole file at once:
 * <ul>
 *     <li>errors (a wrong value, a missing required property, ...) end in an {@link InvalidConfigException}</li>
 *     <li>warnings (a property that does not apply to the function) are returned, the property is ignored</li>
 * </ul>
 */
public final class CentralUnitConfigReader {
    private static final String TYPE = "type";
    private static final String COMPONENTS_TYPES = "componentsTypes";
    private static final String NUMBER = "number";
    private static final List<String> TOP_LEVEL_PROPERTIES = List.of(TYPE, COMPONENTS_TYPES);

    private static final Map<Function, Class<? extends ComponentSpec>> FUNCTION_SPECS = new EnumMap<>(Map.ofEntries(
            Map.entry(Function.RELAY, RelaySpec.class),
            Map.entry(Function.DIMMER, DimmerSpec.class),
            Map.entry(Function.MOTOR, MotorSpec.class),
            Map.entry(Function.LOCMOOD, LocalMoodSpec.class),
            Map.entry(Function.TIMEDMOOD, TimedMoodSpec.class),
            Map.entry(Function.GENMOOD, GeneralMoodSpec.class),
            Map.entry(Function.FLAG, FlagSpec.class),
            Map.entry(Function.COND, ConditionSpec.class),
            Map.entry(Function.INPUT, InputSpec.class),
            Map.entry(Function.TIMEDFNC, TimedFunctionSpec.class),
            Map.entry(Function.DISPLAYMESSAGE, DisplayMessageSpec.class)
    ));

    // STRING is left out on purpose: the bridge creates those sensors itself
    private static final Map<SensorType, Class<? extends SensorSpec>> SENSOR_SPECS = new EnumMap<>(Map.of(
            SensorType.TEMPERATURE, TemperatureSensorSpec.class,
            SensorType.LIGHT, LightSensorSpec.class,
            SensorType.HUMIDITY, HumiditySensorSpec.class,
            SensorType.GAS, GasSensorSpec.class,
            SensorType.TEMPERATURECONTROL, TemperatureControlSpec.class,
            SensorType.PULSECOUNTER, PulseCounterSpec.class
    ));

    // Only the fields annotated with @JsonProperty are config properties, everything else of a ComponentSpec is runtime state
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setVisibility(PropertyAccessor.ALL, Visibility.NONE);

    private final String source;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private CentralUnitConfigReader(String source) {
        this.source = source;
    }

    /**
     * @param source Where the config comes from, only used in the messages.
     * @throws InvalidConfigException With all errors of the config file.
     */
    public static Result read(InputStream inputStream, String source) {
        return new CentralUnitConfigReader(source).read(inputStream);
    }

    /**
     * @param centralUnit The central unit of a valid config file.
     * @param warnings    The problems that were ignored, one line each.
     */
    public record Result(CentralUnit centralUnit, List<String> warnings) {
    }

    private Result read(InputStream inputStream) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(inputStream);
        } catch (JsonProcessingException e) {
            throw invalid("is not valid JSON" + at(e.getLocation()) + ": " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the config file " + this.source, e);
        }
        if (root == null || !root.isObject()) {
            throw invalid("must contain a JSON object with \"type\" and \"componentsTypes\"");
        }

        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(this.centralUnitType(root.get(TYPE)));
        centralUnit.setComponentsTypes(this.components(root.get(COMPONENTS_TYPES)));
        root.fieldNames().forEachRemaining(name -> {
            if (!TOP_LEVEL_PROPERTIES.contains(name)) {
                this.warnings.add("'" + name + "' is not a property of the config file, it is ignored (known properties: " + TOP_LEVEL_PROPERTIES + ")");
            }
        });

        if (!this.errors.isEmpty()) {
            throw new InvalidConfigException(this.source, this.errors);
        }
        return new Result(centralUnit, List.copyOf(this.warnings));
    }

    private InvalidConfigException invalid(String error) {
        return new InvalidConfigException(this.source, List.of("The file " + error));
    }

    private static String at(JsonLocation location) {
        return location == null ? "" : " at line " + location.getLineNr() + ", column " + location.getColumnNr();
    }

    private CentralUnitType centralUnitType(JsonNode type) {
        String expected = ", expected one of " + Arrays.toString(CentralUnitType.values());
        if (type == null || type.isNull()) {
            this.errors.add("'type' is required" + expected);
            return null;
        }
        return Arrays.stream(CentralUnitType.values()).filter(t -> t.name().equalsIgnoreCase(type.asText())).findAny().orElseGet(() -> {
            this.errors.add("'type' " + type + " is not a central unit type" + expected);
            return null;
        });
    }

    private Map<Function, List<ComponentSpec>> components(JsonNode componentsTypes) {
        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        if (componentsTypes == null || !componentsTypes.isObject()) {
            this.errors.add("'componentsTypes' is required, an object with a list of components per function, e.g. {\"RELAY\": [{\"number\": 1, \"description\": \"Kitchen\"}]}");
            return components;
        }
        componentsTypes.fields().forEachRemaining(entry -> {
            Optional<Function> function = Arrays.stream(Function.values()).filter(f -> f.name().equalsIgnoreCase(entry.getKey())).findAny();
            if (function.isEmpty()) {
                this.errors.add("componentsTypes: '" + entry.getKey() + "' is not a function, expected one of " + Arrays.toString(Function.values()));
            } else if (!entry.getValue().isArray()) {
                this.errors.add("componentsTypes." + entry.getKey() + " must be a list of components, [ ... ]");
            } else {
                components.computeIfAbsent(function.get(), f -> new ArrayList<>()).addAll(this.components(function.get(), entry.getValue()));
            }
        });
        return components;
    }

    private List<ComponentSpec> components(Function function, JsonNode nodes) {
        List<ComponentSpec> components = new ArrayList<>();
        Map<Integer, String> numbers = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            ComponentProblems problems = new ComponentProblems(label(function, i, node));
            Optional.ofNullable(this.component(function, node, problems)).ifPresent(component -> {
                String other = node.hasNonNull(NUMBER) ? numbers.putIfAbsent(component.getNumber(), problems.label) : null;
                if (other != null) {
                    problems.error(NUMBER, component.getNumber() + " is already used by " + other);
                }
                components.add(component);
            });
        }
        return components;
    }

    /**
     * @return {@code null} when the problems leave no component to validate.
     */
    private ComponentSpec component(Function function, JsonNode node, ComponentProblems problems) {
        if (!node.isObject()) {
            problems.error(null, "must be an object, { ... }");
            return null;
        }
        if (!node.hasNonNull(NUMBER)) {
            problems.error(NUMBER, "is required");
        }
        Class<? extends ComponentSpec> specType = function == Function.SENSOR ? sensorSpec(node.get(TYPE), problems) : FUNCTION_SPECS.get(function);
        if (specType == null) {
            return null;
        }
        ObjectNode properties = ((ObjectNode) node).deepCopy();
        if (function == Function.SENSOR) {
            // The type picked the class, it is not a property of the class
            properties.remove(TYPE);
        }

        ObjectReader reader = OBJECT_MAPPER.readerFor(specType).withHandler(new UnknownPropertyWarning(function, problems));
        ComponentSpec component;
        try {
            component = reader.readValue(properties);
        } catch (MismatchedInputException e) {
            String property = propertyName(e);
            problems.error(property, properties.get(property) + " is not " + expected(e.getTargetType()));
            return null;
        } catch (JsonMappingException e) {
            problems.error(propertyName(e), e.getOriginalMessage());
            return null;
        } catch (IOException e) {
            problems.error(null, e.getMessage());
            return null;
        }
        component.validate(problems);
        return component;
    }

    private static Class<? extends SensorSpec> sensorSpec(JsonNode type, ConfigProblems problems) {
        String expected = ", expected one of " + SENSOR_SPECS.keySet();
        if (type == null || type.isNull()) {
            problems.error(TYPE, "is required for a SENSOR" + expected);
            return null;
        }
        return SENSOR_SPECS.entrySet().stream().filter(e -> e.getKey().name().equalsIgnoreCase(type.asText())).map(Map.Entry::getValue).findAny().orElseGet(() -> {
            problems.error(TYPE, type + " is not a sensor type" + expected);
            return null;
        });
    }

    private static String propertyName(JsonMappingException e) {
        return e.getPath().isEmpty() ? null : e.getPath().getLast().getFieldName();
    }

    private static String expected(Class<?> type) {
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "a whole number";
        } else if (type == double.class || type == Double.class || type == BigDecimal.class) {
            return "a number";
        } else if (type == boolean.class || type == Boolean.class) {
            return "true or false";
        } else if (type == String.class) {
            return "a text";
        }
        return "a valid " + (type == null ? "value" : type.getSimpleName());
    }

    /**
     * E.g. {@code SENSOR #3 (number 321, "GAS - 4-20ma")}, taken from the JSON so it also works when the component could not be read.
     */
    private static String label(Function function, int index, JsonNode node) {
        List<String> identity = new ArrayList<>();
        if (node.hasNonNull(NUMBER)) {
            identity.add("number " + node.get(NUMBER).asText());
        }
        if (node.hasNonNull("description")) {
            identity.add("\"" + node.get("description").asText() + "\"");
        }
        return function + " #" + (index + 1) + (identity.isEmpty() ? "" : " (" + String.join(", ", identity) + ")");
    }

    private final class ComponentProblems implements ConfigProblems {
        private final String label;

        private ComponentProblems(String label) {
            this.label = label;
        }

        @Override
        public void error(String property, String message) {
            errors.add(this.message(property, message));
        }

        @Override
        public void warning(String property, String message) {
            warnings.add(this.message(property, message));
        }

        private String message(String property, String message) {
            return this.label + ": " + (property == null ? "" : "'" + property + "' ") + message;
        }
    }

    /**
     * A property that does not apply to the function, a typo or a leftover of an older version, should not keep the bridge from starting.
     */
    private static final class UnknownPropertyWarning extends DeserializationProblemHandler {
        private final Function function;
        private final ConfigProblems problems;

        private UnknownPropertyWarning(Function function, ConfigProblems problems) {
            this.function = function;
            this.problems = problems;
        }

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
            String known = deserializer.getKnownPropertyNames().stream().map(String::valueOf).sorted().collect(Collectors.joining(", "));
            this.problems.warning(propertyName, "is not a property of a " + this.function + this.sensorType(beanOrClass) + ", it is ignored (known properties: " + known + ")");
            p.skipChildren();
            return true;
        }

        private String sensorType(Object beanOrClass) {
            return beanOrClass instanceof SensorSpec sensor ? " of type " + sensor.getSensorType() : "";
        }
    }
}
