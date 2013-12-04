package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectState;
import org.mobicents.servlet.restcomm.rvd.dto.Step;

import com.google.gson.Gson;

public class BuildService {
	
	public static void buildProject( ProjectState projectState, String projectPath) throws IOException {
		for ( ProjectState.Node node : projectState.getNodes() ) {
			buildNode( node, projectPath );
		}
	}
	
	private static void buildNode( ProjectState.Node node, String projectPath) throws IOException {
		System.out.println("building node " + node.getName() );
		
		// TODO sanitize node name!
		File outFile = new File( projectPath + "data/" + node.getName() + ".node" );
		Gson gson = new Gson();
		FileUtils.writeStringToFile(outFile, gson.toJson(node.getStepnames()), "UTF-8");
		
		// process the steps one-by-one
		for ( String stepname : node.getSteps().keySet() ) {
			Step step = node.getSteps().get(stepname);
				FileUtils.writeStringToFile(new File(projectPath +"data/" + node.getName() + "." + step.getName()), gson.toJson(step), "UTF-8");			
		}
	}
}
