package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;

import org.mobicents.servlet.restcomm.rvd.dto.ProjectItem;




@Path("/manager/projects")
public class RvdManagerResource  {
	
	private static final String workspaceDirectoryName = "workspace"; // TODO Maybe load this from...somewhere
	
	@Context ServletContext servletContext;

	@GET @Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listProjects() {
		ProjectItem item = new ProjectItem();
		item.setName("Phonecard2");
		item.setStartupUrl("http://localhost:8080/rvdservices/controller/appname/start");
		List<ProjectItem> items = new ArrayList<ProjectItem>();
		items.add(item);
		items.add(item);
		
		//System.out.println( "servletContext: " + servletContext.getRealPath(File.separator) );
		  
		Gson gson = new Gson(); // TODO - maybe inject this and create it all the time. See https://bitbucket.org/telestax/telscale-restcomm/src/dec355993594e902f3155324f49b57ee76727548/restcomm/restcomm.http/src/main/java/org/mobicents/servlet/restcomm/http/IncomingPhoneNumbersEndpoint.java?at=ts713#cl-242
		return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
	}
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response createProject(@QueryParam("name") String name) {
		System.out.println("project name: " + name);
		
		// TODO IMPORTANT!!! sanitize the project name!!
		
		String directoryPath = servletContext.getRealPath(File.separator) + workspaceDirectoryName + File.separator + name;
		System.out.println( "project directory path: " + directoryPath );
		
		// Copy the _proto project directory as a new project
		/*File newDir = new File(directoryPath);
		if ( !newDir.exists() ) {
			if ( newDir.mkdir() ) {
				//newDir = new File(directoryPath + File.separator + "data");
				//if ( newDir.mkdir() )
					
				return Response.ok().build();
			}
		}
		*/
		
		return Response.status(Status.BAD_REQUEST).build(); // TODO This is not the correct return code for all cases of error 
	}
}
