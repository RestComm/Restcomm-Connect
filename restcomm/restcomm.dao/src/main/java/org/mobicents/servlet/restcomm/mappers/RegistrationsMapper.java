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
public interface RegistrationsMapper {

    String INSERT_REGISTRATION="INSERT INTO \"restcomm_registrations\" (\"sid\", \"date_created\", \"date_updated\", "
        + "\"date_expires\", \"address_of_record\", \"display_name\",\"user_name\", \"user_agent\", \"ttl\",  "
        + "\"location\", \"webrtc\", \"instanceid\",\"isLBPresent\") "
        + "VALUES "
        + "(#{sid}, #{date_created}, #{date_updated}, "
        + "#{date_expires}, #{address_of_record}, #{display_name}, #{user_name}, #{user_agent}, #{ttl}, "
        + "#{location}, #{webrtc}, #{instanceid}, #{isLBPresent})";
    String SELECT_REGISTRATIONS="SELECT * FROM \"restcomm_registrations\"";
    String SELECT_REGISTRATION="SELECT * FROM \"restcomm_registrations\" WHERE \"user_name\"=#{userName}";
    String SELECT_REGISTRATION_BY_INSTANCEID="SELECT * FROM \"restcomm_registrations\" WHERE \"user_name\"=#{user_name} AND \"instanceid\"=#{instanceid}";
    String HAS_REGISTRATION="SELECT COUNT(*) FROM \"restcomm_registrations\" WHERE \"address_of_record\"=#{address_of_record} AND \"display_name\"=#{display_name} AND \"location\"=#{location} AND \"user_agent\"=#{user_agent}";
    String DELETE_REGISTRATION="DELETE FROM \"restcomm_registrations\" WHERE \"location\"=#{location} AND \"address_of_record\"=#{address_of_record}";
    String UPDATE_REGISTRATION="UPDATE \"restcomm_registrations\" SET \"ttl\"=#{ttl}, \"date_expires\"=#{date_expires}, \"date_updated\"=#{date_updated} WHERE \"address_of_record\"=#{address_of_record} AND \"display_name\"=#{display_name} AND \"location\"=#{location} AND \"user_agent\"=#{user_agent}";

    @Insert(INSERT_REGISTRATION)
    void addRegistration(Map map);

    @Select(SELECT_REGISTRATION)
    List<Map<String, Object>> getRegistration(String userName);

    @Select(SELECT_REGISTRATIONS)
    List<Map<String, Object>> getRegistrations();

    @Select(SELECT_REGISTRATION_BY_INSTANCEID)
    List<Map<String, Object>> getRegistrationByInstanceId(Map map);

    @Select(HAS_REGISTRATION)
    int hasRegistration(Map map);

    @Delete(DELETE_REGISTRATION)
    void removeRegistration(Map map);

    @Update(UPDATE_REGISTRATION)
    void updateRegistration(Map map);

}
