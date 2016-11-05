package be.xhibit.teletask.webapp.rest.component;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.webapp.ClientHolder;
import be.xhibit.teletask.webapp.rest.ResourceSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/component")
public class ComponentResourceSupport extends ResourceSupport {
    private static final ObjectWriter PRETTY_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    /**
     * The TDSClient object, which makes the IP socket connection to the TDS hardware.
     * The ComponentResource is loaded a a singleton.  This way we know only one instance of the TDSClient is created, and only one client is accessing
     * the TDS domotics central unit.
     */
    private TeletaskClient client;

    public ComponentResourceSupport() {
    }

    /**
     * Gets the complete config in JSON.
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/config
     *
     * @return JSON representation of the complete Teletask config in place..
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/config")
    public Response config() throws JsonProcessingException {
        return this.buildSuccessResponse(WRITER.writeValueAsString(this.getClient().getConfig()));
    }

    /**
     * Gets a partial config in JSON.
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/config/{function}
     *
     * @return JSON representation of the Teletask config in place..
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/config/{function}")
    public Response config(@PathParam("function") String function) throws JsonProcessingException {
        return this.buildSuccessResponse(WRITER, this.getClient().getConfig().getComponents(Function.valueOf(function.toUpperCase())));
    }

    /**
     * Gets the complete config in JSON.
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/pretty-config
     *
     * @return JSON response confirming if the switch request was successful, together with the correct state.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pretty-config")
    public Response prettyConfig() throws JsonProcessingException {
        return this.buildSuccessResponse(PRETTY_WRITER, this.getClient().getConfig());
    }

    /**
     * Gets the status of a specific function in JSON.
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/{function}/{number}
     *
     * @return JSON response representing the correct state.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{function}/{number}")
    public Response component(@PathParam("function") String function, @PathParam("number") int number) throws JsonProcessingException {
        APIResponse response = new APIResponse("success", this.getClient().getConfig().getComponent(Function.valueOf(function.toUpperCase()), number));
        return this.buildSuccessResponse(PRETTY_WRITER, response);
    }

    /**
     * Gets the status of a specific function in JSON, but using a forced refresh (?).
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/refresh/{function}/{number}
     *
     * @return JSON response representing the correct state.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/refresh/{function}/{number}")
    public Response refresh(@PathParam("function") String function, @PathParam("number") int number) {
        ComponentSpec component = this.getClient().getConfig().getComponent(Function.valueOf(function.toUpperCase()), number);
        this.getClient().get(component);
        APIResponse response = new APIResponse("success", component);
        return this.buildSuccessResponse(PRETTY_WRITER, response);
    }

    /**
     * Gets the simple state of a specific function, either 0|1.
     * Method can be used in services expecting this simple type of response, like the HomeBridge and home-assistant.io projects.
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/state/{function}/{number}
     *
     * @return Value representing the state: 0 for "off", 1 for "on".
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/state/{function}/{number}")
    public Response state(@PathParam("function") String function, @PathParam("number") int number) {
        ComponentSpec component = this.getClient().getConfig().getComponent(Function.valueOf(function.toUpperCase()), number);
        String stateValue = component.getState();
        Integer state = "ON".equalsIgnoreCase(stateValue) ? 1 : 0;
        return this.buildSuccessResponse(state);
    }

    /**
     * Switches the status of a specific function into the desired state.
     * URI: (GET) {protocol}://{host}:{port}/{context}/api/component/{function}/{number}/state/{state}
     * Note: should actually be a PUT, but used a GET for easier testing purpose.
     *
     * @return JSON response representing the correct component state.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{function}/{number}/state/{state}")
    public Response component(@PathParam("function") String function, @PathParam("number") int number, @PathParam("state") String state) {
        Function functionEnum = Function.valueOf(function.toUpperCase());

        return this.set(functionEnum, number, state);
    }

    private Response get(int number, Function function) {
        return this.buildGetResponse(this.getClient().getConfig().getComponent(function, number));
    }

    private Response set(Function function, int number, String state) {
        this.getClient().set(function, number, state);

        return this.buildGetResponse(this.getClient().getConfig().getComponent(function, number));
    }

    private Response buildGetResponse(ComponentSpec... component) {
        APIResponse apiResponse = new APIResponse("success", component);
        return this.buildSuccessResponse(PRETTY_WRITER, apiResponse);
    }

    private TeletaskClient getClient() {
        while (this.client == null) {
            this.setClient(this.createClient());
        }
        return this.client;
    }

    protected TeletaskClient createClient() {
        return ClientHolder.getClient();
    }

    private void setClient(TeletaskClient client) {
        this.client = client;
    }
}
