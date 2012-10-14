package org.mobicents.servlet.sip.restcomm.entities;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
@NotThreadSafe
public final class AnnouncementList {
	  private final List<Announcement> announcements;

	  public AnnouncementList(final List<Announcement> announcements) {
	    super();
	    this.announcements = announcements;
	  }
	  
	  public List<Announcement> getAnnouncements() {
	    return announcements;
	  }
}
