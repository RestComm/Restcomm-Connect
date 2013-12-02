package org.mobicents.servlet.restcomm.rvd;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import org.mobicents.servlet.restcomm.rvd.dto.ProjectItem;




@Path("/manager/projects/")
public class RvdManagerResource  {

	@GET @Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listProjects() {
		ProjectItem item = new ProjectItem();
		item.setName("Phonecard2");
		item.setStartupUrl("http://localhost:8080/rvdservices/controller/appname/start");
		List<ProjectItem> items = new ArrayList<ProjectItem>();
		items.add(item);
		items.add(item);
		
	  
		Gson gson = new Gson(); // TODO - maybe inject this and create it all the time. See https://bitbucket.org/telestax/telscale-restcomm/src/dec355993594e902f3155324f49b57ee76727548/restcomm/restcomm.http/src/main/java/org/mobicents/servlet/restcomm/http/IncomingPhoneNumbersEndpoint.java?at=ts713#cl-242
		return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
	}
}
