package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Mapper<V extends Enum<V>> {
    private static final Logger LOG = LogManager.getLogger();

    private final Map<V, Number> byName = new HashMap<>();
    private final Map<Integer, V> byNumber = new HashMap<>();
    private final Class<V> enumClass;
    private final NumberConverter numberConverter;

    public Mapper(Class<V> enumClass, NumberConverter numberConverter) {
        this.enumClass = enumClass;
        this.numberConverter = numberConverter;
    }

    public Mapper<V> add(V name, Number value) {
        StateMapping<V> mapping = new StateMapping<>(name, value);
        this.byName.put(mapping.getName(), mapping.getWrite());
        this.byNumber.put(mapping.getRead().intValue(), mapping.getName());
        this.byNumber.put(mapping.getWrite().intValue(), mapping.getName());
        return this;
    }

    public boolean contains(V key) {
        return this.byName.containsKey(key);
    }

    public V toEnum(byte[] dataBytes) {
        return Enum.valueOf(enumClass, toString(dataBytes));
    }

    public String toString(byte[] dataBytes) {
        int key = this.numberConverter.convert(dataBytes).intValue();
        LOG.trace(() -> "Converted '" + Bytes.bytesToHex(dataBytes) + "' to number '" + key + "'. Looking up the key in " + this.byNumber);
        return Optional.ofNullable(this.byNumber.get(key)).map(Objects::toString).orElseThrow(() -> new IllegalStateException(String.format("No value for key '%s' that was extracted from following bytes: '%s' using '%s'. The mapper has following keys: %s", key, Bytes.bytesToHex(dataBytes), this.numberConverter.getClass().getName(), this.byNumber)));
    }

    public byte[] toBytes(V value) {
        Number number = this.byName.get(value);
        return this.numberConverter.convert(number);
    }
}
