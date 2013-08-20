package org.mobicents.servlet.restcomm.http.voipinnovations.converter;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.http.voipinnovations.LATA;
import org.mobicents.servlet.restcomm.http.voipinnovations.State;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class StateConverter extends AbstractConverter {
  public StateConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return State.class.equals(klass);
  }

  @Override public Object unmarshal(final HierarchicalStreamReader reader,
      final UnmarshallingContext context) {
    String name = null;
    final List<LATA> latas = new ArrayList<LATA>();
    while(reader.hasMoreChildren()) {
      reader.moveDown();
      final String child = reader.getNodeName();
      if("name".equals(child)) {
        name = reader.getValue();
      } else if("lata".equals(child)) {
        final LATA lata = (LATA)context.convertAnother(null, LATA.class);
        latas.add(lata);
      }
      reader.moveUp();
    }
    return new State(name, latas);
  }
}
