package org.mobicents.servlet.restcomm.rvd.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UnauthorizedException;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CallControlAction;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CallControlStatus;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CreateCallResponse;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;

import com.google.gson.Gson;

public class RestService {

    @Context
    HttpServletRequest request;

    protected Response buildErrorResponse(Response.Status httpStatus, RvdResponse.Status rvdStatus, RvdException exception) {
        RvdResponse rvdResponse = new RvdResponse(rvdStatus).setException(exception);
        return Response.status(httpStatus).entity(rvdResponse.asJson()).build();
    }

    protected  Response buildInvalidResponse(Response.Status httpStatus, RvdResponse.Status rvdStatus, ValidationReport report ) {
        RvdResponse rvdResponse = new RvdResponse(rvdStatus).setReport(report);
        return Response.status(httpStatus).entity(rvdResponse.asJson()).build();
    }

    //Response buildInvalidResponse(Response.Status httpStatus, RvdResponse.Status rvdStatus, RvdException exception ) {
    //    RvdResponse rvdResponse = new RvdResponse(rvdStatus).setException(exception);
    //    return Response.status(httpStatus).entity(rvdResponse.asJson()).build();
    //}

    protected  Response buildOkResponse() {
        RvdResponse rvdResponse = new RvdResponse( RvdResponse.Status.OK );
        return Response.status(Response.Status.OK).entity(rvdResponse.asJson()).build();
    }

    protected  Response buildOkResponse(Object payload) {
        RvdResponse rvdResponse = new RvdResponse().setOkPayload(payload);
        return Response.status(Response.Status.OK).entity(rvdResponse.asJson()).build();
    }

    protected int size(InputStream stream) {
        int length = 0;
        try {
            byte[] buffer = new byte[2048];
            int size;
            while ((size = stream.read(buffer)) != -1) {
                length += size;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return length;

    }

    protected String read(InputStream stream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();

    }

    protected void secureByRole(String role, AccessToken accessToken) throws UnauthorizedException {
        Set<String> roleNames;
        try {
            roleNames = accessToken.getRealmAccess().getRoles();
        } catch (NullPointerException e) {
            throw new UnauthorizedException("No access token present or no roles in it");
        }

        if ( roleNames.contains(role) ) {
            return;
        } else
            throw new UnauthorizedException();
    }

    protected AccessToken getKeycloakAccessToken() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = session.getToken();
        return accessToken;
    }

    protected Response buildWebTriggerHtmlResponse(String title, String action, String outcome, String description, Integer status ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html><body>");
        if (title != null)
            buffer.append("<h1>").append(title).append("</h1>");
        if (action != null) {
            buffer.append("<h4>").append(action);
            if (outcome != null)
                buffer.append(" - " + outcome);
            buffer.append("</h4>");
        }
        if (description != null)
            buffer.append("<p>").append(description).append("</p>");
        buffer.append("</body></html>");

        return Response.status(status).entity(buffer.toString()).type(MediaType.TEXT_HTML).build();
    }

    protected Response buildWebTriggerJsonResponse(CallControlAction action, CallControlStatus status, Integer httpStatus, Object restcommResponse ) {
        CreateCallResponse response = new CreateCallResponse().setAction(action).setStatus(status).setData(restcommResponse);
        Gson gson = new Gson();
        return Response.status(httpStatus).entity( gson.toJson(response)).type(MediaType.APPLICATION_JSON).build();
    }
}
