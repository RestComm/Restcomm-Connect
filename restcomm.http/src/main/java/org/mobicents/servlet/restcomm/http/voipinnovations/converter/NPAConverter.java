package org.mobicents.servlet.restcomm.http.voipinnovations.converter;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.http.voipinnovations.NPA;
import org.mobicents.servlet.restcomm.http.voipinnovations.NXX;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class NPAConverter extends AbstractConverter {
  public NPAConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return NPA.class.equals(klass);
  }

  @Override public Object unmarshal(final HierarchicalStreamReader reader,
      final UnmarshallingContext context) {
    String name = null;
    final List<NXX> nxxs = new ArrayList<NXX>();
    while(reader.hasMoreChildren()) {
      reader.moveDown();
      final String child = reader.getNodeName();
      if("name".equals(child)) {
        name = reader.getValue();
      } else if("nxx".equals(child)) {
        final NXX nxx = (NXX)context.convertAnother(null, NXX.class);
        nxxs.add(nxx);
      }
      reader.moveUp();
    }
    return new NPA(name, nxxs);
  }
}
