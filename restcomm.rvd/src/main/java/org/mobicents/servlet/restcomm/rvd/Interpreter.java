package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.dto.GatherStep;
import org.mobicents.servlet.restcomm.rvd.dto.ProjectState;
import org.mobicents.servlet.restcomm.rvd.dto.SayStep;
import org.mobicents.servlet.restcomm.rvd.dto.Step;
import org.mobicents.servlet.restcomm.rvd.interpreter.Target;
import org.mobicents.servlet.restcomm.rvd.model.RcmlGatherStep;
import org.mobicents.servlet.restcomm.rvd.model.RcmlResponse;
import org.mobicents.servlet.restcomm.rvd.model.RcmlSayStep;
import org.mobicents.servlet.restcomm.rvd.model.RcmlStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.XStream;


public class Interpreter {
	
	private XStream xstream;
	private Gson gson;
	
	public Interpreter() {
		xstream = new XStream();
		xstream.alias("Response", RcmlResponse.class);
		xstream.addImplicitCollection(RcmlResponse.class, "steps");
		xstream.alias("Say", RcmlSayStep.class);
		xstream.alias("Gather", RcmlGatherStep.class);
		Gson gson = new GsonBuilder().registerTypeAdapter(Step.class, new JsonDeserializer<Step>() {
			@Override
			public Step deserialize(JsonElement rootElement, Type arg1, 
					JsonDeserializationContext arg2) throws JsonParseException {
				
				JsonObject step_object = rootElement.getAsJsonObject();
				String kind = step_object.get("kind").getAsString();
				
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
		}).create();
	}
	
	public String interpret(String targetParam, String projectBasePath) {
		RcmlResponse rcmlModel = new RcmlResponse();
		StringBuffer stringResponse = new StringBuffer();
		
		Target target = Interpreter.parseTarget(targetParam);
		// TODO make sure all the required components of the target are available here
		
		
		try {
			String nodefile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/" + target.getNodename() + ".node"));
			List<String> nodeStepnames = gson.fromJson(nodefile_json, new TypeToken<List<String>>(){}.getType());
			
			// if no starting step has been specified in the target, use the first step of the node as default
			if ( target.getStepname() == null && !nodeStepnames.isEmpty() )
				target.setStepname(nodeStepnames.get(0));
			
			boolean startstep_found = false;
			for ( String stepname : nodeStepnames ) {
				
				if ( stepname.equals(target.getStepname() ) )
					startstep_found = true;
				
				if ( startstep_found )
				{
					System.out.println("starting rendering ");
					
					// we found our starting step. Let's start processing
					String stepfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/" + target.getNodename() + "." + stepname ));
					Step step = gson.fromJson(stepfile_json, Step.class);					
					rcmlModel.steps.add( renderStep(step) );
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return  xstream.toXML(rcmlModel);  
	}

	public RcmlStep renderStep( Step step ) {
		if ( "say".equals(step.getKind()) ) {
			return renderSayStep( (SayStep) step);
		} else
		if ( "gather".equals(step.getKind()) ) {
			return renderGatherStep( (GatherStep) step);
		} else
			return null; // TODO Raise an exception here!
	}
	
	public RcmlSayStep renderSayStep( SayStep step ) {
		
		RcmlSayStep sayStep = new RcmlSayStep();
		sayStep.setPhrase(step.getPhrase());
		
		return sayStep;
	}
	
	public RcmlGatherStep renderGatherStep( GatherStep step ) {
		
		RcmlGatherStep rcmlStep = new RcmlGatherStep();
		rcmlStep.setAction(step.getAction());
		
		return rcmlStep;
	}
	
	public static Target parseTarget( String targetParam ) {
		Target target = new Target();
		
		// TODO accept only valid characters in the target i.e. alphanumeric
		
		Pattern pattern = Pattern.compile("^([^.]+)(.([^.]+))?(.([^.]+))?");
		Matcher matcher = pattern.matcher(targetParam);
		if ( matcher.find() ) {
			if ( matcher.groupCount() >= 1 )
				target.setNodename(matcher.group(1));
			if ( matcher.groupCount() >= 3 )
				target.setStepname(matcher.group(3));
			if ( matcher.groupCount() >= 5 )
				target.setAction(matcher.group(5));			
		}
		
		return target;		
	}
}
