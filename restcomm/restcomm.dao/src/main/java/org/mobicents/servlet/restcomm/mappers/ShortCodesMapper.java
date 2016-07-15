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
public interface ShortCodesMapper {

    String INSERT_SHORTCODE="INSERT INTO \"restcomm_short_codes\" (\"sid\", \"date_created\", \"date_updated\", \"friendly_name\", \"account_sid\", \"short_code\", \"api_version\", \"sms_url\", \"sms_method\",\"sms_fallback_url\", \"sms_fallback_method\", \"uri\") VALUES (#{sid}, #{date_created}, #{date_updated}, #{friendly_name}, #{account_sid}, #{short_code},#{api_version}, #{sms_url}, #{sms_method}, #{sms_fallback_url}, #{sms_fallback_method}, #{uri})";
    String SELECT_SHORTCODE="SELECT * FROM \"restcomm_short_codes\" WHERE \"sid\"=#{sid}";
    String SELECT_SHORTCODES="SELECT * FROM \"restcomm_short_codes\" WHERE \"account_sid\"=#{accountSid}";
    String DELETE_SHORTCODE="DELETE FROM \"restcomm_short_codes\" WHERE \"sid\"=#{sid}";
    String DELETE_SHORTCODES="DELETE FROM \"restcomm_short_codes\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_SHORTCODE="UPDATE \"restcomm_short_codes\" SET \"date_updated\"=#{date_updated}, \"friendly_name\"=#{friendly_name}, \"api_version\"=#{api_version}, \"sms_url\"=#{sms_url},\"sms_method\"=#{sms_method}, \"sms_fallback_url\"=#{sms_fallback_url}, \"sms_fallback_method\"=#{sms_fallback_method} WHERE \"sid\"=#{sid}";

    @Insert(INSERT_SHORTCODE)
    void addShortCode(Map map);

    @Select(SELECT_SHORTCODE)
    Map<String, Object> getShortCode(String sid);

    @Select(SELECT_SHORTCODES)
    List<Map<String, Object>> getShortCodes(String accountSid);

    @Delete(DELETE_SHORTCODE)
    void removeShortCode(String sid);

    @Delete(DELETE_SHORTCODES)
    void removeShortCodes(String accountSid);

    @Update(UPDATE_SHORTCODE)
    void updateShortCode(Map map);

}
