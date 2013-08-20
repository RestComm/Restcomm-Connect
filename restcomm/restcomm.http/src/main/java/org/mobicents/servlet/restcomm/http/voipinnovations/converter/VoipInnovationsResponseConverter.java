package org.mobicents.servlet.restcomm.http.voipinnovations.converter;

import org.mobicents.servlet.restcomm.http.voipinnovations.VoipInnovationsBody;
import org.mobicents.servlet.restcomm.http.voipinnovations.VoipInnovationsHeader;
import org.mobicents.servlet.restcomm.http.voipinnovations.VoipInnovationsResponse;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class VoipInnovationsResponseConverter extends AbstractConverter {
  public VoipInnovationsResponseConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return VoipInnovationsResponseConverter.class.equals(klass);
  }

  @Override public Object unmarshal(final HierarchicalStreamReader reader,
      final UnmarshallingContext context) {
    VoipInnovationsHeader header = null;
    VoipInnovationsBody body = null;
    while(reader.hasMoreChildren()) {
      reader.moveDown();
      final String child = reader.getNodeName();
      if("header".equals(child)) {
        header = (VoipInnovationsHeader)context.convertAnother(null, VoipInnovationsHeader.class);
      } else if("body".equals(child)) {
        body = (VoipInnovationsBody)context.convertAnother(null, VoipInnovationsBody.class);
      }
      reader.moveUp();
    }
    return new VoipInnovationsResponse(header, body);
  }
}
