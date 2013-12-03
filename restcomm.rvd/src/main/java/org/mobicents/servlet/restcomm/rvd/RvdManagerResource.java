package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

import org.mobicents.servlet.restcomm.rvd.dto.ActiveProjectInfo;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectState;




@Path("/manager/projects")
public class RvdManagerResource  {
	
	private static final String workspaceDirectoryName = "workspace"; // TODO Maybe load this from...somewhere
	private static final String protoDirectoryName = "_proto"; // the prototype project directory name
	private static final String projectSessionAttribute = "project"; // the name of the session variable where the active project will be stored
	
	@Context ServletContext servletContext;

	@GET @Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listProjects() {
		
        String workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName + File.separator;
        System.out.println( "workspacePath: " + workspaceBasePath);
        File workspaceDir = new File(workspaceBasePath);
        if ( !workspaceDir.exists() )
        	;
        	//TODO handle the error;
        
        File[] entries = workspaceDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File anyfile) {
            	if ( anyfile.isDirectory() && !anyfile.getName().equals(protoDirectoryName) )
            		return true;
            	return false;
            }
        });
        Arrays.sort(entries, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            } 
        });
        
		List<ProjectItem> items = new ArrayList<ProjectItem>();
        for ( File entry : entries ) {
    		System.out.println( "entry: " + entry.getName() );
    		ProjectItem item = new ProjectItem();
    		item.setName(entry.getName());
    		item.setStartupUrl(entry.getName());
    		items.add(item);
        }
				  
		Gson gson = new Gson(); // TODO - maybe inject this and create it all the time. See https://bitbucket.org/telestax/telscale-restcomm/src/dec355993594e902f3155324f49b57ee76727548/restcomm/restcomm.http/src/main/java/org/mobicents/servlet/restcomm/http/IncomingPhoneNumbersEndpoint.java?at=ts713#cl-242
		return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
	}
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response createProject(@QueryParam("name") String name) {
		
		// TODO IMPORTANT!!! sanitize the project name!!
		
		String directoryBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName + File.separator;
		File sourceDir = new File(directoryBasePath + "_proto");
		File destDir = new File(directoryBasePath + name);
		if ( !destDir.exists() ) {
			try {
				FileUtils.copyDirectory(sourceDir, destDir);
				return Response.ok().build();
			} catch (IOException e) {
				// TODO - do some logging or custom handling
				e.printStackTrace();
			}
		}

		return Response.status(Status.BAD_REQUEST).build(); // TODO This is not the correct return code for all cases of error 
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateProject(@Context HttpServletRequest request) {
		String projectName = (String) request.getSession().getAttribute(projectSessionAttribute);
		if ( projectName != null && !projectName.equals("") ) {
			System.out.println("saveProject " + projectName);
			String workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName + File.separator;
			
			FileOutputStream stateFile_os;
			try {
				stateFile_os = new FileOutputStream(workspaceBasePath + projectName + File.separator + "state");
				IOUtils.copy(request.getInputStream(), stateFile_os);
				stateFile_os.close();
				return Response.ok().build(); 
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}	
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response openProject(@QueryParam("name") String name, @Context HttpServletRequest request) {

		// TODO CAUTION!!! sanitize name
		// ...
		
		String workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName + File.separator;
		File stateFile = new File( workspaceBasePath + name + File.separator + "state");
		try {
			FileInputStream stateFileStream = new FileInputStream(stateFile);
			request.getSession().setAttribute(projectSessionAttribute, name); // mark the open project in the session
			return Response.ok().entity(stateFileStream).build();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return Response.status(Status.BAD_REQUEST).build(); // TODO This is not the correct return code for all cases of error
	}
	
	@GET @Path("/active")
	public Response getActiveProject(@Context HttpServletRequest request) {
		String name = (String) request.getSession().getAttribute(projectSessionAttribute);
		if ( name != null && !name.equals("") ) {
			ActiveProjectInfo projectInfo = new ActiveProjectInfo();
			projectInfo.setName(name);
			
			Gson gson = new Gson(); // TODO - maybe inject this and create it all the time. See https://bitbucket.org/telestax/telscale-restcomm/src/dec355993594e902f3155324f49b57ee76727548/restcomm/restcomm.http/src/main/java/org/mobicents/servlet/restcomm/http/IncomingPhoneNumbersEndpoint.java?at=ts713#cl-242
			return Response.ok(gson.toJson(projectInfo), MediaType.APPLICATION_JSON).build();
		}
		return Response.status(Status.NOT_FOUND).build(); 
	}
	
	@GET @Path("/close")
	public Response closeActiveProject(@Context HttpServletRequest request) {
		if ( request.getSession().getAttribute(projectSessionAttribute) != null )
			request.getSession().removeAttribute(projectSessionAttribute);
		
		return Response.ok().build(); 	
	}
	
	@POST @Path("/build")
	public Response buildProject(@Context HttpServletRequest request) {
		String name = (String) request.getSession().getAttribute(projectSessionAttribute);
		if ( name != null && !name.equals("") ) {
			
			String workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName + File.separator;
			File projectDir = new File( workspaceBasePath + name);
			
			if ( projectDir.exists() ) {
				
				String projectPath = workspaceBasePath + name + File.separator;
				File dataDir = new File( projectPath + "data");

				// delete all files in directory
				for ( File anyfile : dataDir.listFiles() ) {
					anyfile.delete();
				}
				
				// and now process state
				try {
					
					String state_json = FileUtils.readFileToString( new File( projectPath + "state"), "UTF-8" );
					System.out.println("state: " + state_json);
					
					ProjectState projectState = new Gson().fromJson(state_json, ProjectState.class);
					System.out.println( "startNodeName: " + projectState.getStartNodeName() );
					BuildService.buildProject(projectState, projectPath);
					
					return Response.ok().build();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(Status.NOT_FOUND).build();
	}
	
}
