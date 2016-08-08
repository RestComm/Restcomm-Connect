package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberFilter;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface IncomingPhoneNumbersMapper {
    String INSERT_INCOMING_PHONE_NUMBER="INSERT INTO \"restcomm_incoming_phone_numbers\" ("
        + "\"sid\", \"date_created\", \"date_updated\", \"friendly_name\", \"account_sid\", "
        + "\"phone_number\", \"api_version\", \"voice_caller_id_lookup\", \"voice_url\", "
        + "\"voice_method\", \"voice_fallback_url\", \"voice_fallback_method\", \"status_callback\", "
        + "\"status_callback_method\", \"voice_application_sid\", \"sms_url\", \"sms_method\", "
        + "\"sms_fallback_url\", \"sms_fallback_method\", \"sms_application_sid\", \"uri\", "
        + "\"voice_capable\", \"sms_capable\", \"mms_capable\", \"fax_capable\", \"pure_sip\", "
        + "\"cost\", \"ussd_url\", \"ussd_method\", \"ussd_fallback_url\", \"ussd_fallback_method\", "
        + "\"ussd_application_sid\") "
        + "VALUES("
        + "#{sid}, #{date_created}, #{date_updated}, #{friendly_name}, #{account_sid}, "
        + "#{phone_number}, #{api_version}, #{voice_caller_id_lookup}, #{voice_url}, #{voice_method}, "
        + "#{voice_fallback_url}, #{voice_fallback_method}, #{status_callback}, #{status_callback_method}, "
        + "#{voice_application_sid}, #{sms_url}, #{sms_method}, #{sms_fallback_url}, #{sms_fallback_method}, "
        + "#{sms_application_sid}, #{uri}, #{voice_capable}, #{sms_capable}, #{mms_capable}, #{fax_capable}, "
        + "#{pure_sip}, #{cost}, #{ussd_url}, #{ussd_method}, #{ussd_fallback_url},  #{ussd_fallback_method}, "
        + "#{ussd_application_sid})";
    String SELECT_INCOMING_PHONE_NUMBER="SELECT * FROM \"restcomm_incoming_phone_numbers\" WHERE \"sid\"=#{sid}";
    String SELECT_INCOMING_PHONE_NUMBER_BY_VALUE="SELECT * FROM \"restcomm_incoming_phone_numbers\" WHERE \"phone_number\"=#{phoneNumber}";
    String SELECT_INCOMING_PHONE_NUMBERS="SELECT * FROM \"restcomm_incoming_phone_numbers\" WHERE \"account_sid\"=#{accountSid}";
    String SELECT_ALL_INCOMING_PHONE_NUMBERS="SELECT * FROM \"restcomm_incoming_phone_numbers\"";
    String SELECT_INCOMING_PHONE_NUMBER_BY_FRIENDLY_NAME="<script>"
        +"SELECT * FROM \"restcomm_incoming_phone_numbers\" "
        + " SELECT"
        + " n.*,"
        + " a_voice.friendly_name voice_application_name,"
        + " a_sms.friendly_name sms_application_name,"
        + " a_ussd.friendly_name ussd_application_name"
        + " FROM restcomm_incoming_phone_numbers n"
        + " LEFT OUTER JOIN \"restcomm_applications\" a_voice  ON n.voice_application_sid = a_voice.sid"
        + " LEFT OUTER JOIN \"restcomm_applications\" a_sms ON n.sms_application_sid = a_sms.sid"
        + " LEFT OUTER JOIN \"restcomm_applications\" a_ussd ON n.ussd_application_sid = a_ussd.sid"
        + " WHERE \"account_sid\"=#{accountSid}"
        + "    <if test=\"friendlyName != null\">"
        + "        AND \"friendly_name\"=#{friendlyName}"
        + "    </if>"
        + "    <if test=\"phoneNumber != null\">"
        + "        AND \"phone_number\" like #{phoneNumber}"
        + "    </if>"
        + "</script>";
    String DELETE_INCOMING_PHONE_NUMBER="DELETE FROM \"restcomm_incoming_phone_numbers\" WHERE \"sid\"=#{sid}";
    String DELETE_INCOMING_PHONE_NUMBERS="DELETE FROM \"restcomm_incoming_phone_numbers\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_INCOMING_PHONE_NUMBER="UPDATE \"restcomm_incoming_phone_numbers\" SET \"friendly_name\"=#{friendly_name}, \"voice_caller_id_lookup\"=#{voice_caller_id_lookup}, \"voice_url\"=#{voice_url}, \"voice_method\"=#{voice_method}, \"voice_fallback_url\"=#{voice_fallback_url}, \"voice_fallback_method\"=#{voice_fallback_method}, \"status_callback\"=#{status_callback}, \"status_callback_method\"=#{status_callback_method}, \"voice_application_sid\"=#{voice_application_sid}, \"sms_url\"=#{sms_url}, \"sms_method\"=#{sms_method}, \"sms_fallback_url\"=#{sms_fallback_url}, \"sms_fallback_method\"=#{sms_fallback_method}, \"sms_application_sid\"=#{sms_application_sid}, \"voice_capable\"=#{voice_capable},  \"sms_capable\"=#{sms_capable}, \"mms_capable\"=#{mms_capable}, \"fax_capable\"=#{fax_capable}, \"ussd_url\"=#{ussd_url}, \"ussd_method\"=#{ussd_method},  \"ussd_fallback_url\"=#{ussd_fallback_url}, \"ussd_fallback_method\"=#{ussd_fallback_method}, \"ussd_application_sid\"=#{ussd_application_sid} WHERE \"sid\"=#{sid}";

    @Insert(INSERT_INCOMING_PHONE_NUMBER)
    void addIncomingPhoneNumber(Map map);

    @Select(SELECT_INCOMING_PHONE_NUMBER)
    Map<String, Object> getIncomingPhoneNumber(String sid);

    @Select(SELECT_INCOMING_PHONE_NUMBER_BY_VALUE)
    Map<String,Object> getIncomingPhoneNumberByValue(String phoneNumber);

    @Select(SELECT_INCOMING_PHONE_NUMBERS)
    List<Map<String, Object>> getIncomingPhoneNumbers(String accountSid);

    @Select(SELECT_ALL_INCOMING_PHONE_NUMBERS)
    List<Map<String, Object>> getAllIncomingPhoneNumbers();

    @Select(SELECT_INCOMING_PHONE_NUMBER_BY_FRIENDLY_NAME)
    List<Map<String, Object>> getIncomingPhoneNumbersByFriendlyName(IncomingPhoneNumberFilter filter);

    @Delete(DELETE_INCOMING_PHONE_NUMBER)
    void removeIncomingPhoneNumber(String sid);

    @Delete(DELETE_INCOMING_PHONE_NUMBERS)
    void removeIncomingPhoneNumbers(String accountSid);

    @Update(UPDATE_INCOMING_PHONE_NUMBER)
    void updateIncomingPhoneNumber(Map map);
}
