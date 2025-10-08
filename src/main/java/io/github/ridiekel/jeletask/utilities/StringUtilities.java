package io.github.ridiekel.jeletask.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.stream.Collectors;

public class StringUtilities {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).findAndRegisterModules();

    private StringUtilities() {
    }

    public static String indent(String prettyMessage) {
        return prettyMessage.lines().map(l -> "\t\t" + l).collect(Collectors.joining("\n"));
    }

    public static String prettyString(String result) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(OBJECT_MAPPER.readTree(result));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
