package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.mobicents.servlet.restcomm.rvd.dto.ProjectItem;

public class ProjectService {

	private ServletContext servletContext; // TODO we have to find way other that directly through constructor parameter. 
	
	// configuration parameters 
	private static final String workspaceDirectoryName = "workspace";
	private static final String protoDirectoryName = "_proto"; // the prototype project directory name	
	
	private String workspaceBasePath;
	
	 ProjectService(ServletContext servletContext) {
		 this.servletContext = servletContext;
		 
		 workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName;
	 }
		
	public String getWorkspaceBasePath() {
		return workspaceBasePath;
	}
	
	public List<ProjectItem> getAvailableProjects() {
		
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
	    		item.setStartupUrl(entry.getName());
	    		items.add(item);
	        }
        }
        return items;
	}

	public boolean projectExists( String projectName ) {
		List<ProjectItem> projects = getAvailableProjects();
		for ( ProjectItem project : projects ) {
			if ( project.getName().equals(projectName) )
				return true;
		}
		return false;
	}
}
