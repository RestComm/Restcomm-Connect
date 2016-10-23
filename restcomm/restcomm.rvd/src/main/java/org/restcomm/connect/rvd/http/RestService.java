package org.restcomm.connect.rvd.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restcomm.connect.rvd.ApplicationContext;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.model.callcontrol.CallControlAction;
import org.restcomm.connect.rvd.model.callcontrol.CallControlStatus;
import org.restcomm.connect.rvd.model.callcontrol.CreateCallResponse;
import org.restcomm.connect.rvd.validation.ValidationReport;

import com.google.gson.Gson;

public class RestService {

    @Context
    protected HttpServletRequest request;
    @Context
    protected ServletContext servletContext;
    protected ApplicationContext applicationContext;

    public RestService() {
    }

    public RestService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected void init() {
        this.applicationContext = (ApplicationContext) servletContext.getAttribute(ApplicationContext.class.getName());
    }

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
