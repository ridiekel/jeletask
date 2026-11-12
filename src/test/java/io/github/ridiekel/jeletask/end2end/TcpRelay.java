package io.github.ridiekel.jeletask.end2end;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Passes TCP connections on to a target. {@link #cut()} drops them all at once, like a crash or a pulled cable: the
 * peers get no goodbye, only a closed socket.
 */
final class TcpRelay implements AutoCloseable {
    private final ServerSocket server;
    private final String targetHost;
    private final int targetPort;
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    TcpRelay(String targetHost, int targetPort) throws IOException {
        this.server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        Thread.ofVirtual().start(this::accept);
    }

    String host() {
        return this.server.getInetAddress().getHostAddress();
    }

    int port() {
        return this.server.getLocalPort();
    }

    void cut() {
        for (Socket socket : this.sockets) {
            try {
                socket.close();
            } catch (IOException _) {
                // Closed already
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.server.close();
        this.cut();
    }

    private void accept() {
        while (!this.server.isClosed()) {
            try {
                Socket client = this.server.accept();
                Socket target = new Socket(this.targetHost, this.targetPort);
                this.sockets.add(client);
                this.sockets.add(target);
                pipe(client, target);
                pipe(target, client);
            } catch (IOException _) {
                return;
            }
        }
    }

    private static void pipe(Socket from, Socket to) {
        Thread.ofVirtual().start(() -> {
            try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
                in.transferTo(out);
            } catch (IOException _) {
                // One side is gone, which is the end of this direction
            }
        });
    }
}
