package be.xhibit.teletask.webapp.websocket;

import be.xhibit.teletask.client.listener.StateChangeListener;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.webapp.ClientHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.List;

@ServerEndpoint("/state-changes")
public class StateChangeListenerEndpoint extends JSONBroadcastingWebSocket {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StateChangeListenerEndpoint.class);

    static {
        ClientHolder.getClient().registerStateChangeListener(new StateChangeListener() {
            @Override
            public void receive(List<ComponentSpec> components) {
                try {
                    sendAll(components);
                } catch (JsonProcessingException e) {
                    LOG.error("Exception ({}) caught in event: {}", e.getClass().getName(), e.getMessage(), e);
                }
            }

            @Override
            public void stop() {

            }
        });
    }

    @Override
    public void open(Session session) {
        super.open(session);
    }

    @Override
    public void error(Session session, Throwable t) {
        super.error(session, t);
    }

    @Override
    public void closedConnection(Session session) {
        super.closedConnection(session);
    }
}