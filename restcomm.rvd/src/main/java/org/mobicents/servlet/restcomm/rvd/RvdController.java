package org.mobicents.servlet.restcomm.rvd;


import java.io.File;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;

import com.google.gson.Gson;



@Path("/apps/{appname}/controller")
public class RvdController {

	// configuration parameters 
	//private static final String workspaceDirectoryName = "workspace";
	//private static final String protoDirectoryName = "_proto"; // the prototype project directory name
	
	private String workspaceBasePath;
	
	@Context ServletContext servletContext;
	private ProjectService projectService; // use a proper way to initialize this in init()
	private Gson gson;
	
	@PostConstruct
	void init() {
		//workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName;
		gson = new Gson();
		projectService = new ProjectService(servletContext);
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response controller( @PathParam("appname") String appname, @QueryParam("target") String targetParam ) {

		if ( !projectService.projectExists(appname) )
			return Response.status(Status.NOT_FOUND).build();
		
		String projectBasePath = projectService.getWorkspaceBasePath() + File.separator + appname;		
		Interpreter interpreter = new Interpreter();
		String rcmlResponse = interpreter.interpret(targetParam, projectBasePath);
		
		System.out.println("result: " + rcmlResponse);
				
		return Response.ok().build();
	}
	
		
}
