package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface SandBoxesMapper {

    String INSERT_SANDBOX="INSERT INTO \"restcomm_sand_boxes\" (\"date_created\", \"date_updated\", \"pin\", \"account_sid\", \"phone_number\", \"application_sid\", \"api_version\", \"voice_url\", \"voice_method\", \"sms_url\", \"sms_method\", \"status_callback\", \"status_callback_method\", \"uri\") VALUES (#{date_created}, #{date_updated}, #{pin}, #{account_sid}, #{phone_number}, #{application_sid}, #{api_version}, #{voice_url}, #{voice_method}, #{sms_url}, #{sms_method}, #{status_callback}, #{status_callback_method}, #{uri})";
    String SELECT_SENDBOX="SELECT * FROM \"restcomm_sand_boxes\" WHERE \"account_sid\"=#{accountSid}";
    String DELETE_SANDBOX="DELETE FROM \"restcomm_sand_boxes\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_SANDBOX="UPDATE \"restcomm_sand_boxes\" SET \"date_updated\"=#{date_updated}, \"voice_url\"=#{voice_url}, \"voice_method\"=#{voice_method}, \"sms_url\"=#{sms_url}, \"sms_method\"=#{sms_method}, \"status_callback\"=#{status_callback}, \"status_callback_method\"=#{status_callback_method} WHERE \"account_sid\"=#{account_sid}";

    @Insert(INSERT_SANDBOX)
    void addSandBox(Map map);

    @Select(SELECT_SENDBOX)
    List<Map<String, Object>> getSandBox(String accountSid);

    @Delete(DELETE_SANDBOX)
    void removeSandBox(String accountSid);

    @Update(UPDATE_SANDBOX)
    void updateSandBox(Map map);
}
