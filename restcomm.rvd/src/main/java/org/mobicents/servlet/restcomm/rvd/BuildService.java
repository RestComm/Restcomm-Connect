package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectState;
import org.mobicents.servlet.restcomm.rvd.dto.Step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class BuildService {
	
	private Gson gson;
	
	
	public BuildService() {
		// Parse the big project state object into a nice dto model
		gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).registerTypeAdapter(Step.class, new StepJsonSerializer()).create();
	}
	
	public void buildProject( String projectStateJson, String projectPath) throws IOException {
		ProjectState projectState = gson.fromJson(projectStateJson, ProjectState.class);
		
		for ( ProjectState.Node node : projectState.getNodes() ) {
			buildNode( node, projectPath );
		}
	}
	
	private void buildNode( ProjectState.Node node, String projectPath) throws IOException {
		System.out.println("building node " + node.getName() );
		
		// TODO sanitize node name!
		File outFile = new File( projectPath + "data/" + node.getName() + ".node" );
		FileUtils.writeStringToFile(outFile, gson.toJson(node.getStepnames()), "UTF-8");
		
		// process the steps one-by-one
		for ( String stepname : node.getSteps().keySet() ) {
			Step step = node.getSteps().get(stepname);
			System.out.println("building step " + step.getKind() + " - " + step.getName());
			FileUtils.writeStringToFile(new File(projectPath +"data/" + node.getName() + "." + step.getName()), gson.toJson(step), "UTF-8");			
		}
	}
}
