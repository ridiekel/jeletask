package be.xhibit.teletask.webapp.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import javax.ws.rs.core.Response;

public abstract class ResourceSupport {
    protected Response buildSuccessResponse(Object response) {
        return Response.status(200).entity(response).header("Access-Control-Allow-Origin", "*").build();
    }

    protected Response buildNotAuthorizedResponse() {
        return Response.status(403).entity("Not Authorized").build();
    }

    public Response buildSuccessResponse(ObjectWriter writer, Object object) {
        try {
            return this.buildSuccessResponse(writer.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
