package org.mobicents.servlet.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.restcomm.entities.Announcement;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public interface AnnouncementsDao {
	public void addAnnouncement(Announcement announcement);
	public Announcement getAnnouncement(Sid sid);
	public List<Announcement> getAnnouncements(Sid accountSid);
	public void removeAnnouncement(Sid sid);
	public void removeAnnouncements(Sid accountSid);
}
