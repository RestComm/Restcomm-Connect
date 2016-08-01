package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 * @author zahid.med@gmail.com (ZAHID Mohammed)
 */
public interface AnnouncementsMapper {

    String SELECT_ANNOUNCEMENT="SELECT * FROM \"restcomm_announcements\" WHERE \"sid\"=#{sid}";
    String SELECT_ANNOUNCEMENTS="SELECT * FROM \"restcomm_announcements\" WHERE \"account_sid\"=#{account_sid}";
    String INSERT_ANNOUNCEMENT="INSERT INTO \"restcomm_announcements\" (\"sid\", \"date_created\", \"account_sid\", \"gender\", \"language\", \"text\", \"uri\")"
        + "VALUES(#{sid}, #{date_created}, #{account_sid}, #{gender}, #{language}, #{text}, #{uri})";
    String DELETE_ANNOUNCEMENT="DELETE FROM \"restcomm_announcements\" WHERE \"sid\"=#{sid}";
    String DELETE_ANNOUNCEMENTS="DELETE FROM \"restcomm_announcements\" WHERE \"account_sid\"=#{accountSid}";

    @Select(SELECT_ANNOUNCEMENT)
    Map<String,Object> getAnnouncement(String sid);

    @Select(SELECT_ANNOUNCEMENTS)
    List<Map<String, Object>> getAnnouncements(String accountSid);

    @Insert(INSERT_ANNOUNCEMENT)
    void addAnnouncement(Map map);

    @Delete(DELETE_ANNOUNCEMENT)
    void removeAnnouncement(String sid);

    @Delete(DELETE_ANNOUNCEMENTS)
    void removeAnnouncements(String accountSid);
}
