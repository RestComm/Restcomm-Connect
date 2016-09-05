package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface SmsMessagesMapper {

    String INSERT_SMS_MESSAGE="INSERT INTO \"restcomm_sms_messages\" (\"sid\", \"date_created\", \"date_updated\", \"date_sent\", \"account_sid\", \"sender\", \"recipient\", \"body\", \"status\", \"direction\", \"price\", \"api_version\", \"uri\") VALUES (#{sid}, #{date_created}, #{date_updated}, #{date_sent}, #{account_sid}, #{sender}, #{recipient}, #{body}, #{status}, #{direction}, #{price}, #{api_version}, #{uri})";
    String SELECT_SMS_MESSAGE="SELECT * FROM \"restcomm_sms_messages\" WHERE \"sid\"=#{sid}";
    String SELECT_SMS_MESSAGES="SELECT * FROM \"restcomm_sms_messages\" WHERE \"account_sid\"=#{accountSid}";
    String DELETE_SMS_MESSAGE="DELETE FROM \"restcomm_sms_messages\" WHERE \"sid\"=#{sid}";
    String DELETE_SMS_MESSAGES="DELETE FROM \"restcomm_sms_messages\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_SMS_MESSAGE="UPDATE \"restcomm_sms_messages\" SET \"date_sent\"=#{date_sent}, \"status\"=#{status}, \"price\"=#{price} WHERE \"sid\"=#{sid}";

    @Insert(INSERT_SMS_MESSAGE)
    void addSmsMessage(Map map);

    @Select(SELECT_SMS_MESSAGE)
    Map<String,Object> getSmsMessage(String sid);

    @Select(SELECT_SMS_MESSAGES)
    List<Map<String,Object>> getSmsMessages(String accountSid);

    @Delete(DELETE_SMS_MESSAGE)
    void removeSmsMessage(String sid);

    @Delete(DELETE_SMS_MESSAGES)
    void removeSmsMessages(String accountSid);

    @Update(UPDATE_SMS_MESSAGE)
    void updateSmsMessage(Map map);
}
