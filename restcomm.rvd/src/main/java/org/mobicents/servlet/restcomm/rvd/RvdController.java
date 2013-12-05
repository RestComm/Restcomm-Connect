package org.mobicents.servlet.restcomm.rvd;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectState;
import org.mobicents.servlet.restcomm.rvd.interpreter.Target;
import org.mobicents.servlet.restcomm.rvd.model.RcmlResponse;
import org.mobicents.servlet.restcomm.rvd.model.RcmlStep;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



@Path("/apps/{appname}/controller")
public class RvdController {

	// configuration parameters 
	//private static final String workspaceDirectoryName = "workspace";
	//private static final String protoDirectoryName = "_proto"; // the prototype project directory name
	
	private String workspaceBasePath;
	
	@Context ServletContext servletContext;
	private ProjectService projectService; // use a proper way to initialize this in init()
	Gson gson;
	
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
				
		return Response.ok("Running application " + appname + ". workspaceBasePath " + workspaceBasePath).build();
	}
	
		
}
