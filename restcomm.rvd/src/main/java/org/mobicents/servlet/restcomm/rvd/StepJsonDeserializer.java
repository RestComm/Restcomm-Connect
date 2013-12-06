package org.mobicents.servlet.restcomm.rvd;

import java.lang.reflect.Type;

import org.mobicents.servlet.restcomm.rvd.dto.DialStep;
import org.mobicents.servlet.restcomm.rvd.dto.GatherStep;
import org.mobicents.servlet.restcomm.rvd.dto.HungupStep;
import org.mobicents.servlet.restcomm.rvd.dto.SayStep;
import org.mobicents.servlet.restcomm.rvd.dto.Step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class StepJsonDeserializer implements JsonDeserializer<Step> {

	@Override
	public Step deserialize(JsonElement rootElement, Type arg1, 
			JsonDeserializationContext arg2) throws JsonParseException {
		
		JsonObject step_object = rootElement.getAsJsonObject();
		String kind = step_object.get("kind").getAsString();
		
		Gson gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).create();
		
		Step step;
		if ( "say".equals(kind) ) {
			step = gson.fromJson(step_object, SayStep.class);
		}
		else if ( "gather".equals(kind) )
			step = gson.fromJson(step_object, GatherStep.class);
		else if ( "dial".equals(kind) )
			step = gson.fromJson(step_object, DialStep.class);		
		else if ( "hungup".equals(kind) )
			step = gson.fromJson(step_object, HungupStep.class);			
		else {
			step = null;
			System.out.println("Error deserializing step. Unknown step found!"); //TODO remove me and return a nice value!!!
		}
						
		return step;
	}
	
}
