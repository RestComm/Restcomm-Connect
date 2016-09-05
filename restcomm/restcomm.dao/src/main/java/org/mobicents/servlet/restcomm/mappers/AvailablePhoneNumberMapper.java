package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface AvailablePhoneNumberMapper {


    String SELECT_AVAILABLE_PHONE_NUMBERS="SELECT * FROM \"restcomm_available_phone_numbers\"";
    String SELECT_PHONE_BY_AREA_CODE="SELECT * FROM \"restcomm_available_phone_numbers\" WHERE \"iso_country\"='US' OR \"iso_country\"='CA' AND \"phone_number\" LIKE #{phoneNumber}";
    String SELECT_PHONE_BY_REGION="SELECT * FROM \"restcomm_available_phone_numbers\" WHERE \"region\"=#{region}";
    String SELECT_PHONE_BY_POSTAL_CODE="SELECT * FROM \"restcomm_available_phone_numbers\" WHERE \"postal_code\"=#{postalCode}";
    String SELECT_PHONE_BY_PATTERN="SELECT * FROM \"restcomm_available_phone_numbers\" WHERE \"phone_number\" LIKE #{phone_number}";
    String DELETE_PHONE_NUMBER="DELETE FROM \"restcomm_available_phone_numbers\" WHERE \"phone_number\"=#{phoneNumber}";
    String INSERT_PHONE_NUMBER="INSERT INTO \"restcomm_available_phone_numbers\" (\"friendly_name\", \"phone_number\", \"lata\", "
        + "\"rate_center\", \"latitude\", \"longitude\", \"region\", \"postal_code\", \"iso_country\", \"cost\",\"voice_capable\",\"sms_capable\",\"mms_capable\",\"fax_capable\") "
        + "VALUES ("
        + "#{friendly_name}, "
        + "#{phone_number}, "
        + "#{lata}, "
        + "#{rate_center}, "
        + "#{latitude}, "
        + "#{longitude}, "
        + "#{region}, "
        + "#{postal_code}, "
        + "#{iso_country}, "
        + "#{cost}, "
        + "#{voice_capable}, "
        + "#{sms_capable}, "
        + "#{mms_capable}, "
        + "#{fax_capable}"
        + ");";

    @Insert(INSERT_PHONE_NUMBER)
    void addAvailablePhoneNumber(Map map);

    @Select(SELECT_AVAILABLE_PHONE_NUMBERS)
    List<Map<String, Object>> getAvailablePhoneNumbers();

    @Select(SELECT_PHONE_BY_AREA_CODE)
    List<Map<String, Object>> getAvailablePhoneNumbersByAreaCode(String phoneNumber);

    @Select(SELECT_PHONE_BY_PATTERN)
    List<Map<String, Object>> getAvailablePhoneNumbersByPattern(String phoneNumber);

    @Select(SELECT_PHONE_BY_REGION)
    List<Map<String, Object>> getAvailablePhoneNumbersByRegion(String region);

    @Select(SELECT_PHONE_BY_POSTAL_CODE)
    List<Map<String, Object>> getAvailablePhoneNumbersByPostalCode(int postalCode);

    @Delete(DELETE_PHONE_NUMBER)
    void removeAvailablePhoneNumber(String phoneNumber);

}
