package io.github.ridiekel.jeletask.server;

import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.builder.message.MessageUtilities;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TeletaskTestServer implements Runnable, TeletaskReceiver {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TeletaskTestServer.class);
    public static final byte[] ACKNOWLEDGE = {10};

    private final int port;
    private final CentralUnit centralUnit;
    private ServerSocket server;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Timer timer = new Timer();

    public TeletaskTestServer(int port, CentralUnit centralUnit) {
        this.port = port;
        this.centralUnit = centralUnit;

        new Thread(this, "teletask-test-server").start();
    }

    @Override
    public void run() {
        try {
            this.server = new ServerSocket(this.getPort());
            this.socket = this.server.accept();
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        List<MessageSupport> messages = MessageUtilities.receive(LOG, TeletaskTestServer.this);
                        for (MessageSupport message : messages) {
                            LOG.debug("Processing message: {}", message.toString());
                            TeletaskTestServer.this.outputStream.write(ACKNOWLEDGE);
                            List<EventMessage> eventMessages = message.respond(TeletaskTestServer.this.getCentralUnit(), TeletaskTestServer.this.getMessageHandler());
                            if (eventMessages != null) {
                                for (EventMessage eventMessage : eventMessages) {
                                    LOG.debug("Sending bytes to client: {}", Bytes.bytesToHex(eventMessage.getRawBytes()));
                                    TeletaskTestServer.this.outputStream.write(eventMessage.getRawBytes());
                                    TeletaskTestServer.this.outputStream.flush();
                                }
                            }
                            TeletaskTestServer.this.outputStream.flush();
                        }
                    } catch (Exception e) {
                        LOG.error("Exception ({}) caught in run: {}", e.getClass().getName(), e.getMessage(), e);
                    }
                }
            }, 100, 10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        LOG.debug("Stopping test server...");
        try {
            this.timer.purge();
            this.timer.cancel();
            this.inputStream.close();
            this.outputStream.close();
            this.socket.close();
            this.server.close();
        } catch (IOException e) {
            LOG.error("Exception ({}) caught in stop: {}", e.getClass().getName(), e.getMessage(), e);
        }
        LOG.debug("Stopped test server.");
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public CentralUnit getCentralUnit() {
        return this.centralUnit;
    }

    @Override
    public MessageHandler getMessageHandler() {
        return MessageHandlerFactory.getMessageHandler(this.getCentralUnit().getCentralUnitType());
    }
}
