package org.mobicents.servlet.restcomm.rvd.http;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.client.SettingsModel;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.thoughtworks.xstream.XStream;

@Path("settings")
public class SettingsRestService extends RestService {
    static final Logger logger = Logger.getLogger(RasRestService.class.getName());

    @Context
    ServletContext servletContext;
    RvdConfiguration settings;
    ModelMarshaler marshaler;

    private Gson gson;
    private XStream xstream;


    @PostConstruct
    void init() {
        settings = RvdConfiguration.getInstance(servletContext);
        marshaler = new ModelMarshaler();
    }

    @POST
    public Response setSettings(@Context HttpServletRequest request) {
        try {
            // Create a settings model from the request
            String data;
            data = IOUtils.toString(request.getInputStream());
            SettingsModel settingsModel = marshaler.toModel(data, SettingsModel.class);

            // Store the model to a .settings file in the root of the workspace
            File settingsFile = new File(settings.getWorkspaceBasePath() + "/.settings");
            FileUtils.writeStringToFile(settingsFile, marshaler.toData(settingsModel));
            return Response.ok().build();
        } catch (IOException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (JsonSyntaxException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettings() {
        try {
            File settingsFile = new File(settings.getWorkspaceBasePath() + "/.settings");

            if ( !settingsFile.exists() )
                return Response.status(Status.NOT_FOUND).build(); // this is a successful response

            String data = FileUtils.readFileToString(settingsFile, Charset.forName("UTF-8"));
            return Response.ok(data, MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
