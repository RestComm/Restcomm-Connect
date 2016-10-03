package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProvider;
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;
import org.mobicents.servlet.restcomm.rvd.model.LoginForm;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.identity.BasicAuthCredentials;

import com.google.gson.Gson;


@Path("auth")
public class LoginRestService extends SecuredRestService {
    //static final Logger logger = Logger.getLogger(LoginRestService.class.getName());

    @PostConstruct
    public void init() {
        super.init();
    }

    LoginRestService(UserIdentityContext context) {
        super(context);
    }

    /**
     * Authenticates username/password passed in an HTTP form against restcomm. Does not really keep state.
     *
     * @param request
     * @return
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response postLogin(@Context HttpServletRequest request) throws IOException { // TODO make sure IOException is handled properly!
        // extract credentials from the request
        String data = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));
        Gson gson = new Gson();
        LoginForm form = gson.fromJson(data,LoginForm.class);

        AccountProvider accounts = applicationContext.getAccountProvider();
        BasicAuthCredentials creds = new BasicAuthCredentials(form.getUsername(),form.getPassword());
        RestcommAccountInfoResponse accountInfo = accounts.getAccount(creds);
        if (accountInfo != null)
            return Response.ok().build();
        else
            return Response.status(Status.UNAUTHORIZED).build();
        // TODO return INTERNAL_SERVER_ERROR in case of non-auth error
    }

    @GET
    @Path("keepalive")
    public Response keepalive() {

        secure();
        return Response.ok().build();
    }

}
