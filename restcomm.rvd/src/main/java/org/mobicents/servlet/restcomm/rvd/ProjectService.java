package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.WavFileItem;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


public class ProjectService {

	private ServletContext servletContext; // TODO we have to find way other that directly through constructor parameter. 
	
	// configuration parameters 
	private static final String workspaceDirectoryName = "workspace";
	private static final String protoDirectoryName = "_proto"; // the prototype project directory name
	
	
	private String workspaceBasePath;
	
	public ProjectService(ServletContext servletContext) {
		 this.servletContext = servletContext;
		 
		 workspaceBasePath = this.servletContext.getRealPath(File.separator) + workspaceDirectoryName;
	 }
		
	public String getWorkspaceBasePath() {
		return workspaceBasePath;
	}
	
	/**
	 * Builds the startUrl for an application based on the application name and the incoming 
	 * httpRequest. It is depending on the initial REST request that called this function.
	 * Usually this httpRequest comes either from user's browser when he runs Admin-UI
	 * or RVD. However, this url will be used in Restcomm too. Make sure that Restcomm can access 
	 * the generated the same way client's browser does.    
	 *  
	 * 
	 * @param projectName
	 * @param httpRequest
	 * @return An absolute url pointing to the starting URL of the application
	 * @throws UnsupportedEncodingException 
	 * @throws URISyntaxException 
	 */
	public static String getStartUrlForProject( String projectName, HttpServletRequest httpRequest ) throws  URISyntaxException {		
		URI startURI = new URI(httpRequest.getScheme(),
				null, 
				httpRequest.getServerName(), 
				(httpRequest.getServerPort() == 80 ? -1 : httpRequest.getServerPort()), 
				httpRequest.getContextPath() + httpRequest.getServletPath() + "/apps/" + projectName + "/controller", 
				null, 
				null 
		);
		
		/*String startUrl = httpRequest.getScheme() + "://" + 
				httpRequest.getServerName() + 
				(httpRequest.getServerPort() == 80 ? "" : (":"+ httpRequest.getServerPort())) + 
				httpRequest.getContextPath() + httpRequest.getServletPath() + "/apps/" + projectName + "/controller"; 
		return startUrl; 
		*/
		
		return startURI.toASCIIString();
	}

	/**
	 * Populates an application list with startup urls for each application 
	 * @param items	
	 * @param httpRequest
	 * @throws URISyntaxException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void fillStartUrlsForProjects( List<ProjectItem> items, HttpServletRequest httpRequest ) throws URISyntaxException  {
		for ( ProjectItem item : items ) {
			item.setStartUrl( getStartUrlForProject(item.getName(), httpRequest) );
		}
	}
	
	public List<ProjectItem> getAvailableProjects() throws BadWorkspaceDirectoryStructure {
		
		List<ProjectItem> items = new ArrayList<ProjectItem>();
		
		File workspaceDir = new File(workspaceBasePath);
        if ( workspaceDir.exists() )
        {
        	
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
	        
			
	        for ( File entry : entries ) {
	    		ProjectItem item = new ProjectItem();
	    		item.setName(entry.getName());
	    		item.setStartUrl(entry.getName());
	    		items.add(item);
	        }
        } else
        	throw new BadWorkspaceDirectoryStructure();
        
        return items;
	}

	public boolean projectExists( String projectName ) throws BadWorkspaceDirectoryStructure {
		List<ProjectItem> projects = getAvailableProjects();
		for ( ProjectItem project : projects ) {
			if ( project.getName().equals(projectName) )
				return true;
		}
		return false;
	}
	
	public void createProject( String projectName ) throws ProjectDirectoryAlreadyExists, IOException {
		
		String workspaceBasePath = getWorkspaceBasePath(); 
		File sourceDir = new File(workspaceBasePath + File.separator + "_proto");
		File destDir = new File(workspaceBasePath + File.separator + projectName);
		if ( !destDir.exists() ) {
			FileUtils.copyDirectory(sourceDir, destDir);
		} else {
			throw new ProjectDirectoryAlreadyExists();
		}
	}
	
	
	
	public List<WavFileItem> getWavs(String appName) throws BadWorkspaceDirectoryStructure {
		List<WavFileItem> items = new ArrayList<WavFileItem>();
		
		File workspaceDir = new File(workspaceBasePath + File.separator + appName + File.separator + "wavs" );
        if ( workspaceDir.exists() )
        {
        	
			File[] entries = workspaceDir.listFiles(new FileFilter() {
	            @Override
	            public boolean accept(File anyfile) {
	            	if ( anyfile.isFile() )
	            		return true;
	            	return false;
	            }
	        });
	        Arrays.sort(entries, new Comparator<File>() {
	            public int compare(File f1, File f2) {
	                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
	            } 
	        });
	        			
	        for ( File entry : entries ) {
	        	WavFileItem item = new WavFileItem();
	        	item.setFilename(entry.getName());
	    		items.add(item);
	        }
        } else
        	throw new BadWorkspaceDirectoryStructure();
        
        return items;
	}
}
