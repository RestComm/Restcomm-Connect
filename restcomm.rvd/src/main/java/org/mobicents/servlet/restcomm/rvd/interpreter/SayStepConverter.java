package org.mobicents.servlet.restcomm.rvd.interpreter;

import org.mobicents.servlet.restcomm.rvd.model.RcmlSayStep;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SayStepConverter implements Converter {

	@Override
	public boolean canConvert(Class elementClass) {
		return elementClass.equals(RcmlSayStep.class);
	}

	@Override
	public void marshal(Object value, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		RcmlSayStep step = (RcmlSayStep) value;
		//writer.startNode("Say");
		writer.setValue(step.getPhrase());
		//writer.endNode();
		
	}

	// will not need this for now
	@Override
	public Object unmarshal(HierarchicalStreamReader arg0,
			UnmarshallingContext arg1) {
		// TODO Auto-generated method stub 
		return null;
	}

}
