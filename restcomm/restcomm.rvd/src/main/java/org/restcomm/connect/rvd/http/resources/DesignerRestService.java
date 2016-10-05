package org.restcomm.connect.rvd.http.resources;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.RvdContext;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.client.WavItem;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

@Path("designer")
public class DesignerRestService extends SecuredRestService {
    static final Logger logger = Logger.getLogger(DesignerRestService.class.getName());

    RvdContext rvdContext;

    @PostConstruct
    public void init() {
        super.init();
        rvdContext = new RvdContext(request, servletContext, applicationContext.getConfiguration());
    }

    public DesignerRestService() {
    }

    DesignerRestService(UserIdentityContext context) {
        super(context);
    }

    /**
     * Returns a list of urls to wav resources bundled with RVD
     * @param name
     * @return
     * @throws StorageException
     * @throws ProjectDoesNotExist
     */
    @GET
    @Path("bundledWavs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBundledWavs(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist {
        secure();
        List<WavItem> items = FsProjectStorage.listBundledWavs(rvdContext);
        return buildOkResponse(items);
    }

}
