package io.github.ridiekel.jeletask.utilities;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BytesTest {
    @Test
    void hexToBytes() {
        String hex = "02 05 03 01 FF 0A";
        Assertions.assertThat(Bytes.bytesToHex(Bytes.hexToBytes(hex))).isEqualTo(hex);
    }


    @Test
    void bytesToHexList() {
        String hex = "02 05 03 01 FF 0A";
        List<String> hexValues = Bytes.bytesToHexList(Bytes.hexToBytes(hex));
        Assertions.assertThat(hexValues).containsExactly(hex.split(" "));
    }
}
