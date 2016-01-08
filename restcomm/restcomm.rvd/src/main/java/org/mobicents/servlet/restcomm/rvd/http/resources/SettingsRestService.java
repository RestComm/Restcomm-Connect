package org.mobicents.servlet.restcomm.rvd.http.resources;

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
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.UserProfile;
import org.mobicents.servlet.restcomm.rvd.model.client.SettingsModel;
import org.mobicents.servlet.restcomm.rvd.security.annotations.RvdAuth;
import org.mobicents.servlet.restcomm.rvd.storage.FsProfileDao;
import org.mobicents.servlet.restcomm.rvd.storage.ProfileDao;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

// TODO rename this to 'profile' as well as method names
@Path("settings")
public class SettingsRestService extends RestService {
    static final Logger logger = Logger.getLogger(RasRestService.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    SecurityContext securityContext;
    RvdConfiguration settings;
    ModelMarshaler marshaler;
    WorkspaceStorage workspaceStorage;

    @PostConstruct
    void init() {
        settings = RvdConfiguration.getInstance();
        marshaler = new ModelMarshaler();
        workspaceStorage = new WorkspaceStorage(settings.getWorkspaceBasePath(), marshaler);
    }

    @RvdAuth
    @POST
    public Response setSettings(@Context HttpServletRequest request) {
        try {
            // Create a settings model from the request
            String data;
            data = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));
            SettingsModel settingsForm = marshaler.toModel(data, SettingsModel.class);
            // update user profile
            ProfileDao profileDao = new FsProfileDao(workspaceStorage);
            String loggedUsername = securityContext.getUserPrincipal().getName();
            UserProfile profile = profileDao.loadUserProfile(loggedUsername);
            if (profile == null)
                profile = new UserProfile();
            profile.setUsername(settingsForm.getApiServerUsername());
            profile.setToken(settingsForm.getApiServerPass());
            profile.setRestcommHost(settingsForm.getApiServerHost());
            profile.setRestcommPort(settingsForm.getApiServerRestPort());
            profileDao.saveUserProfile(loggedUsername, profile);
            return Response.ok().build();
        } catch (IOException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (JsonSyntaxException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RvdAuth
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettings() {
        // load user profile
        ProfileDao profileDao = new FsProfileDao(workspaceStorage);
        String loggedUsername = securityContext.getUserPrincipal().getName();
        UserProfile profile = profileDao.loadUserProfile(loggedUsername);

        SettingsModel settingsForm = new SettingsModel();
        if (profile != null) {
            settingsForm.setApiServerUsername(profile.getUsername());
            settingsForm.setApiServerPass(profile.getToken());
            settingsForm.setApiServerHost(profile.getRestcommHost());
            settingsForm.setApiServerRestPort(profile.getRestcommPort());
        }

        // build a response
        Gson gson = new Gson();
        String data = gson.toJson(settingsForm);
        return Response.ok(data, MediaType.APPLICATION_JSON).build();
    }

}
