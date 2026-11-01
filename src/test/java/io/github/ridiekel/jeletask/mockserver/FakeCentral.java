package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A central unit on a real socket that, unlike {@link TeletaskMockServer}, can lose its connections, go offline and
 * come back on the same port. It needs no Spring context and answers nothing but what a test tells it to.
 */
public final class FakeCentral implements AutoCloseable {
    private static final MessageHandler HANDLER = MessageHandlerFactory.getMessageHandler(CentralUnitType.MICROS_PLUS);
    private static final byte[] NO_RESPONSE = new byte[0];

    private final List<Frame> frames = new CopyOnWriteArrayList<>();
    private final List<Connection> connections = new CopyOnWriteArrayList<>();
    private final int port;
    private volatile ServerSocket server;
    private volatile boolean acknowledging = true;
    private volatile java.util.function.Function<Frame, byte[]> responder = frame -> NO_RESPONSE;

    public FakeCentral() {
        try (ServerSocket reserved = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            this.port = reserved.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int port() {
        return this.port;
    }

    public FakeCentral goOnline() {
        try {
            ServerSocket listening = new ServerSocket();
            listening.setReuseAddress(true);
            listening.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), this.port));
            this.server = listening;
            Thread.ofPlatform().daemon().name("fake-central-accept").start(() -> accept(listening));
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void goOffline() {
        closeQuietly(this.server);
        dropConnections();
    }

    public void dropConnections() {
        this.connections.forEach(connection -> closeQuietly(connection.socket));
    }

    public void acknowledging(boolean acknowledging) {
        this.acknowledging = acknowledging;
    }

    /**
     * Bytes to send right after the acknowledge of a frame, usually a raw event.
     */
    public void respondWith(java.util.function.Function<Frame, byte[]> responder) {
        this.responder = responder;
    }

    public void noResponses() {
        this.responder = frame -> NO_RESPONSE;
    }

    public void send(byte[] bytes) {
        write(this.connections.getLast().socket, bytes);
    }

    public int connectionCount() {
        return this.connections.size();
    }

    public boolean closedByClient(int connection) {
        return this.connections.size() > connection && this.connections.get(connection).closedByClient;
    }

    public void clearFrames() {
        this.frames.clear();
    }

    public List<Frame> frames(Command command) {
        return this.frames.stream().filter(frame -> frame.command() == command).toList();
    }

    public List<Frame> frames(int connection) {
        return this.frames.stream().filter(frame -> frame.connection() == connection).toList();
    }

    public List<Command> commands() {
        return this.frames.stream().map(Frame::command).toList();
    }

    @Override
    public void close() {
        goOffline();
    }

    private void accept(ServerSocket listening) {
        while (!listening.isClosed()) {
            try {
                Socket socket = listening.accept();
                Connection connection = new Connection(this.connections.size(), socket);
                this.connections.add(connection);
                Thread.ofPlatform().daemon().name("fake-central-read").start(() -> read(connection));
            } catch (IOException e) {
                return;
            }
        }
    }

    private void read(Connection connection) {
        try (DataInputStream input = new DataInputStream(connection.socket.getInputStream())) {
            while (true) {
                int stx = input.read();
                if (stx < 0) {
                    connection.closedByClient = true;
                    return;
                }
                int length = input.readUnsignedByte();
                byte[] bytes = new byte[length + 1];
                bytes[0] = (byte) stx;
                bytes[1] = (byte) length;
                input.readFully(bytes, 2, bytes.length - 2);
                handle(connection, new Frame(connection.index, bytes));
            }
        } catch (IOException e) {
            // Either dropped by the test, which closed the socket itself, or reset by the client
            connection.closedByClient = !connection.socket.isClosed();
        }
    }

    private void handle(Connection connection, Frame frame) {
        // Decided before the frame shows up, so a test that saw it knows what happens to it whatever it changes next
        boolean acknowledge = this.acknowledging;
        this.frames.add(frame);
        if (acknowledge) {
            write(connection.socket, new byte[]{(byte) HANDLER.getAcknowledgeValue()});
        }
        byte[] response = this.responder.apply(frame);
        if (response.length > 0) {
            write(connection.socket, response);
        }
    }

    private static void write(Socket socket, byte[] bytes) {
        try {
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
        } catch (IOException e) {
            // The other side is gone. A test about that notices on its own.
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // Nothing left to clean up
        }
    }

    public record Frame(int connection, byte[] bytes) {
        private static final int COMMAND_INDEX = 2;
        private static final int LOG_FUNCTION_INDEX = 3;
        private static final int FUNCTION_INDEX = 4;

        public Command command() {
            return HANDLER.getCommand(this.bytes[COMMAND_INDEX]);
        }

        public Function function() {
            return HANDLER.getFunction(this.bytes[command() == Command.LOG ? LOG_FUNCTION_INDEX : FUNCTION_INDEX]);
        }

        public String hex() {
            return HexFormat.of().formatHex(this.bytes);
        }
    }

    private static final class Connection {
        private final int index;
        private final Socket socket;
        private volatile boolean closedByClient;

        private Connection(int index, Socket socket) {
            this.index = index;
            this.socket = socket;
        }
    }
}
