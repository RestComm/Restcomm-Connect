package org.mobicents.servlet.restcomm.http.voipinnovations.converter;

import org.mobicents.servlet.restcomm.http.voipinnovations.NPA;
import org.mobicents.servlet.restcomm.http.voipinnovations.RateCenter;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import java.util.List;
import java.util.ArrayList;

public final class RateCenterConverter extends AbstractConverter {
  public RateCenterConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return RateCenter.class.equals(klass);
  }

  @Override public Object unmarshal(final HierarchicalStreamReader reader,
      final UnmarshallingContext context) {
    String name = null;
    final List<NPA> npas = new ArrayList<NPA>();
    while(reader.hasMoreChildren()) {
      reader.moveDown();
      final String child = reader.getNodeName();
      if("name".equals(child)) {
        name = reader.getValue();
      } else if("npa".equals(child)) {
        final NPA npa = (NPA)context.convertAnother(null, NPA.class);
        npas.add(npa);
      }
      reader.moveUp();
    }
    return new RateCenter(name, npas);
  }
}
