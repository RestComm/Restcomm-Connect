package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

@Path("designer")
public class DesignerRestService extends RestService {
    static final Logger logger = Logger.getLogger(DesignerRestService.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    SecurityContext securityContext;
    @Context
    HttpServletRequest request;

    RvdContext rvdContext;

    public DesignerRestService() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    void init() {
        rvdContext = new RvdContext(request, servletContext);
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
        List<WavItem> items = FsProjectStorage.listBundledWavs(rvdContext);
        return buildOkResponse(items);
    }

}
