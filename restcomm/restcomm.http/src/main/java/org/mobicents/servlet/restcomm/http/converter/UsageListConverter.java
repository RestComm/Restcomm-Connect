/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.http.converter;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Usage;
import org.mobicents.servlet.restcomm.entities.UsageList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author brainslog@gmail.com (Alexandre Mendonca)
 */
@ThreadSafe
public final class UsageListConverter extends AbstractConverter {
  public UsageListConverter(final Configuration configuration) {
    super(configuration);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean canConvert(final Class klass) {
    return UsageList.class.equals(klass);
  }

  @Override
  public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
    final UsageList list = (UsageList) object;
    writer.startNode("UsageRecords");
    for (final Usage usage : list.getUsages()) {
      context.convertAnother(usage);
    }
    writer.endNode();
  }
}
