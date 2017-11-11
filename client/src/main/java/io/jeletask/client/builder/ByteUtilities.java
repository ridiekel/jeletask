package io.jeletask.client.builder;

import java.util.ArrayList;
import java.util.List;

public class ByteUtilities {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private ByteUtilities() {
    }

    public static String bytesToHex(byte... bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars).trim();
    }

    public static List<String> bytesToHexList(byte... bytes) {
        List<String> hexValues = new ArrayList<>();
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            hexValues.add(new String(new char[]{HEX_ARRAY[v >>> 4], HEX_ARRAY[v & 0x0F]}));
        }
        return hexValues;
    }


}
