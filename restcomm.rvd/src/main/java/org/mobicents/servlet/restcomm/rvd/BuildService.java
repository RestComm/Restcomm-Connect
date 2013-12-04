package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectState;
import org.mobicents.servlet.restcomm.rvd.dto.Step;
import org.mobicents.servlet.restcomm.rvd.dto.SayStep;
import org.mobicents.servlet.restcomm.rvd.dto.GatherStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


public class BuildService {
	
	public static void buildProject( ProjectState projectState, String projectPath) throws IOException {
		for ( ProjectState.Node node : projectState.getNodes() ) {
			buildNode( node, projectPath );
		}
	}
	
	public static void buildProject( String projectStateJson, String projectPath) throws IOException {
		
		// Parse the big project state object into a nice dto model
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Step.class, new JsonDeserializer<Step>() {
			@Override
			public Step deserialize(JsonElement rootElement, Type arg1, 
					JsonDeserializationContext arg2) throws JsonParseException {
				
				JsonObject step_object = rootElement.getAsJsonObject();
				String kind = step_object.get("kind").getAsString();
				
				System.out.println("a step found with kind " + kind );
				Gson gson = new Gson();
				Step step;
				if ( "say".equals(kind) )
					step = gson.fromJson(step_object, SayStep.class);
				else if ( "gather".equals(kind) )
					step = gson.fromJson(step_object, GatherStep.class);
				else
					step = null;
								
				return step;
			}
		});
		Gson gson = gsonBuilder.create();
		ProjectState projectState = gson.fromJson(projectStateJson, ProjectState.class);
		
		
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
