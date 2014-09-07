package org.mobicents.servlet.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.restcomm.entities.Announcement;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public interface AnnouncementsDao {
    void addAnnouncement(Announcement announcement);

    Announcement getAnnouncement(Sid sid);

    List<Announcement> getAnnouncements(Sid accountSid);

    void removeAnnouncement(Sid sid);

    void removeAnnouncements(Sid accountSid);
}
