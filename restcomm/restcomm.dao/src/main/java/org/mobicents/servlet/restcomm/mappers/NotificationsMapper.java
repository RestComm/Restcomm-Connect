package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 *  @author thomas.quintana@telestax.com (Thomas Quintana)
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface NotificationsMapper {

    String INSERT_NOTIFICATION="INSERT INTO \"restcomm_notifications\" (\"sid\", \"date_created\", "
        + "\"date_updated\", \"account_sid\", \"call_sid\", \"api_version\", \"log\", \"error_code\", "
        + "\"more_info\", \"message_text\", \"message_date\", \"request_url\", \"request_method\", "
        + "\"request_variables\", \"response_headers\", \"response_body\", \"uri\") "
        + "VALUES ("
        + "#{sid}, #{date_created}, #{date_updated},#{account_sid}, #{call_sid}, #{api_version}, "
        + "#{log}, #{error_code}, #{more_info}, #{message_text}, #{message_date}, #{request_url}, "
        + "#{request_method},#{request_variables}, #{response_headers}, #{response_body}, #{uri})";
    String SELECT_NOTIFICATION="SELECT * FROM \"restcomm_notifications\" WHERE \"sid\"=#{sid}";
    String SELECT_NOTIFICATION_BY_ACCOUNT="SELECT * FROM \"restcomm_notifications\" WHERE \"account_sid\"=#{accountSid}";
    String SELECT_NOTIFICATION_BY_CALL="SELECT * FROM \"restcomm_notifications\" WHERE \"call_sid\"=#{callSid}";
    String SELECT_NOTIFICATION_BY_LOG_LEVEL="SELECT * FROM \"restcomm_notifications\" WHERE \"log\"=#{log}";
    String SELECT_NOTIFICATION_BY_MESSAGE_DATe="SELECT * FROM \"restcomm_notifications\" WHERE \"message_date\" >= #{start_date} AND \"message_date\" <= #{end_date}";
    String DELETE_NOTIFICATION="DELETE FROM \"restcomm_notifications\" WHERE \"sid\"=#{sid}";
    String DELETE_NOTIFICATION_BY_ACCOUNT="DELETE FROM \"restcomm_notifications\" WHERE \"account_sid\"=#{accountSid}";
    String DELETE_NOTIFICATION_BY_CALL="DELETE FROM \"restcomm_notifications\" WHERE \"call_sid\"=#{callSid}";

    @Insert(INSERT_NOTIFICATION)
    void addNotification(Map map);

    @Select(SELECT_NOTIFICATION)
    Map<String, Object> getNotification(String sid);

    @Select(SELECT_NOTIFICATION_BY_ACCOUNT)
    List<Map<String, Object>> getNotifications(String accountSid);

    @Select(SELECT_NOTIFICATION_BY_CALL)
    List<Map<String, Object>> getNotificationsByCall(String callSid);

    @Select(SELECT_NOTIFICATION_BY_LOG_LEVEL)
    List<Map<String, Object>> getNotificationsByLogLevel(int log);

    @Select(SELECT_NOTIFICATION_BY_MESSAGE_DATe)
    List<Map<String, Object>> getNotificationsByMessageDate(Map map);

    @Delete(DELETE_NOTIFICATION)
    void removeNotification(String sid);

    @Delete(DELETE_NOTIFICATION_BY_ACCOUNT)
    void removeNotifications(String accountSid);

    @Delete(DELETE_NOTIFICATION_BY_CALL)
    void removeNotificationsByCall(String callSid);

}
