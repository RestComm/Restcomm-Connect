package org.restcomm.connect.dao;

import java.util.List;

import org.restcomm.connect.dao.entities.Announcement;
import org.restcomm.connect.commons.dao.Sid;

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
