package io.github.ridiekel.jeletask.client.builder.composer.config;

import java.nio.ByteBuffer;

public enum NumberConverter {
    UNSIGNED_BYTE(1, new Converter() {
        @Override
        public Number toNumber(ByteBuffer buffer) {
            return buffer.get() & 0xff;
        }

        @Override
        public Number convert(String value) {
            return value == null ? null : Short.valueOf(value);
        }

        @Override
        public Number cast(Number number) {
            return number.byteValue();
        }

        @Override
        public ByteBuffer putBytes(ByteBuffer buffer, Number number) {
            return buffer.put((byte) (number.shortValue() & 0xff));
        }
    }),
    UNSIGNED_SHORT(2, new Converter() {
        @Override
        public Number toNumber(ByteBuffer buffer) {
            return buffer.getShort() & 0xffff;
        }

        @Override
        public Number convert(String value) {
            return value == null ? null : Short.valueOf(value);
        }

        @Override
        public Number cast(Number number) {
            return number.shortValue();
        }

        @Override
        public ByteBuffer putBytes(ByteBuffer buffer, Number number) {
            return buffer.putShort((short) (number.intValue() & 0xffff));
        }
    }),

    UNSIGNED_INT(4, new Converter() {
        @Override
        public Number toNumber(ByteBuffer buffer) { return buffer.getInt() & 0xffffffff; }

        @Override
        public Number convert(String value) { return value == null ? null : Integer.valueOf(value); }

        @Override
        public Number cast(Number number) { return number.intValue(); }

        @Override
        public ByteBuffer putBytes(ByteBuffer buffer, Number number) {
            return buffer.putInt((int) (number.intValue() & 0xffffffff));
        }
    });

    private final int byteSize;
    private final Converter converter;

    NumberConverter(int byteSize, Converter converter) {
        this.byteSize = byteSize;
        this.converter = converter;
    }

    public byte[] read(byte[] source, int startIndex) {
        byte[] read = new byte[this.byteSize];
        System.arraycopy(source, startIndex, read, 0, this.byteSize);
        return read;
    }

    public Number convert(byte[] bytes) {
        byte[] toConvert = bytes;
        if (bytes.length > this.byteSize) {
            toConvert = this.read(bytes, 0);
        }
        return this.converter.toNumber(ByteBuffer.wrap(toConvert));
    }

    public byte[] convert(Number number) {
        return this.converter.putBytes(ByteBuffer.allocate(this.byteSize), this.converter.cast(number)).array();
    }

    public byte[] convert(String value) {
        return this.convert(this.converter.convert(value));
    }

    public int getByteSize() {
        return byteSize;
    }

    private interface Converter {
        Number toNumber(ByteBuffer buffer);

        ByteBuffer putBytes(ByteBuffer buffer, Number number);

        Number convert(String value);

        Number cast(Number number);
    }
}
