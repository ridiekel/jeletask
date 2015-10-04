package be.xhibit.teletask.webapp.rest.component;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.webapp.ClientHolder;

import javax.ws.rs.Path;

@Path("/component")
public class ComponentResource extends ComponentResourceSupport {
    @Override
    protected TeletaskClient createClient() {
        return ClientHolder.getClient();
    }
}
