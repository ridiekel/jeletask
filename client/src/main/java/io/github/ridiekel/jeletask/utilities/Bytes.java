package io.github.ridiekel.jeletask.utilities;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public class Bytes {
    public static final byte[] EMPTY = new byte[0];

    public static byte[] concat(byte[]... bytes) {
        ByteBuffer bb = ByteBuffer.allocate(Arrays.stream(bytes).mapToInt(b -> b.length).sum());
        Arrays.stream(bytes).forEach(bb::put);
        return bb.array();
    }

    public static String bytesToHex(int read, byte... bytes) {
        return HexFormat.ofDelimiter(" ").formatHex(bytes, 0, read).toUpperCase();
    }

    public static String bytesToHex(byte... bytes) {
        return bytesToHex(bytes.length, bytes);
    }

    public static byte[] hexToBytes(String s) {
        return HexFormat.of().parseHex(s.replaceAll(" ", ""));
    }

    public static List<String> bytesToHexList(byte... bytes) {
        return Arrays.asList(bytesToHex(bytes).split(" "));
    }
}
