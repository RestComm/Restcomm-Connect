package org.mobicents.servlet.sip.restcomm.http.converter;

import org.apache.http.annotation.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.AccountList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@ThreadSafe public final class AccountListConverter extends AbstractConverter {
  public AccountListConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return AccountList.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final AccountList list = (AccountList)object;
  }
}
