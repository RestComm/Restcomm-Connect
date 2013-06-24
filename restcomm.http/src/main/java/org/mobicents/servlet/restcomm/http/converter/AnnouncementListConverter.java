package org.mobicents.servlet.restcomm.http.converter;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Announcement;
import org.mobicents.servlet.restcomm.entities.AnnouncementList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@ThreadSafe
public class AnnouncementListConverter extends AbstractConverter {

	public AnnouncementListConverter(Configuration configuration) {
		super(configuration);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class klass) {
		return AnnouncementList.class.equals(klass);
	}

	@Override
	public void marshal(final Object object, final HierarchicalStreamWriter writer,
			final MarshallingContext context) {
		final AnnouncementList list = (AnnouncementList)object;
		writer.startNode("Announcements");
		for(final Announcement anno : list.getAnnouncements()) {
			context.convertAnother(anno);
		}
		writer.endNode();
	}
}
