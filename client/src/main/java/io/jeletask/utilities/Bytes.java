package io.jeletask.utilities;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Bytes {
    public static byte[] concat(byte[]... bytes) {
        ByteBuffer bb = ByteBuffer.allocate(Arrays.stream(bytes).mapToInt(b -> b.length).sum());
        Arrays.stream(bytes).forEach(bb::put);
        return bb.array();
    }
}
