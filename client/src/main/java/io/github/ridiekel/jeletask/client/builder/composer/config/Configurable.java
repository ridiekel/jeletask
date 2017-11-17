package io.github.ridiekel.jeletask.client.builder.composer.config;

public abstract class Configurable<T> {
    private final int number;
    private final T object;

    public Configurable(int number, T object) {
        this.number = number;
        this.object = object;
    }

    public T getObject() {
        return this.object;
    }

    public int getNumber() {
        return this.number;
    }

    public byte[] getBytes() {
        return new byte[]{(byte) this.number};
    }

    @Override
    public String toString() {
        return "Configurable{" + "number=" + this.number +
                ", object=" + this.object +
                '}';
    }
}
