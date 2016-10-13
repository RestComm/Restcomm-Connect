package org.restcomm.connect.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.mappers.AnnouncementsMapper;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.AnnouncementsDao;
import org.restcomm.connect.dao.entities.Announcement;
import org.restcomm.connect.dao.entities.Sid;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */

public final class MybatisAnnouncementsDao implements AnnouncementsDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.AnnouncementsDao.";
    private final SqlSessionFactory sessions;

    public MybatisAnnouncementsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addAnnouncement(Announcement announcement) {
        final SqlSession session = sessions.openSession();
        try {
            AnnouncementsMapper mapper=session.getMapper(AnnouncementsMapper.class);
            mapper.addAnnouncement(toMap(announcement));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Announcement getAnnouncement(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            AnnouncementsMapper mapper=session.getMapper(AnnouncementsMapper.class);
            final Map<String, Object> result = mapper.getAnnouncement(sid.toString());
            if (result != null) {
                return toAnnouncement(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Announcement> getAnnouncements(Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            AnnouncementsMapper mapper=session.getMapper(AnnouncementsMapper.class);
            final List<Map<String, Object>> results = mapper.getAnnouncements(accountSid.toString());
            final List<Announcement> announcements = new ArrayList<Announcement>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    announcements.add(toAnnouncement(result));
                }
            }
            return announcements;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeAnnouncement(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            AnnouncementsMapper mapper=session.getMapper(AnnouncementsMapper.class);
            mapper.removeAnnouncement(sid.toString());
            session.commit();
         } finally {
             session.close();
         }
    }

    @Override
    public void removeAnnouncements(Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            AnnouncementsMapper mapper=session.getMapper(AnnouncementsMapper.class);
            mapper.removeAnnouncement(accountSid.toString());
            session.commit();
            } finally {
                session.close();
            }
    }

    private Map<String, Object> toMap(final Announcement announcement) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(announcement.getSid()));
        map.put("date_created", DaoUtils.writeDateTime(announcement.getDateCreated()));
        map.put("account_sid", DaoUtils.writeSid(announcement.getAccountSid()));
        map.put("gender", announcement.getGender());
        map.put("language", announcement.getLanguage());
        map.put("text", announcement.getText());
        map.put("uri", DaoUtils.writeUri(announcement.getUri()));
        return map;
    }

    private Announcement toAnnouncement(final Map<String, Object> map) {
        final Sid sid = DaoUtils.readSid(map.get("sid"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final Sid accountSid = DaoUtils.readSid(map.get("account_sid"));
        final String gender = DaoUtils.readString(map.get("gender"));
        final String language = DaoUtils.readString(map.get("language"));
        final String text = DaoUtils.readString(map.get("text"));
        final URI uri = DaoUtils.readUri(map.get("uri"));
        return new Announcement(sid, dateCreated, accountSid, gender, language, text, uri);
    }

}
