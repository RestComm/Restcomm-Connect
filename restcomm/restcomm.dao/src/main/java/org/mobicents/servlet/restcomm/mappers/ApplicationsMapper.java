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
public interface ApplicationsMapper {

    String SELECT_APPLICATIN="SELECT * FROM \"restcomm_applications\" WHERE \"sid\"=#{sid}";
    String SELECT_APPLICATION_BY_FRIENDLY_NAME="SELECT * FROM \"restcomm_applications\" WHERE \"friendly_name\"=#{friendlyName}";
    String SELECT_APPLICATINS="SELECT * FROM \"restcomm_applications\" WHERE \"account_sid\"=#{accountSid} or \"account_sid\" is null";
    String INSERT_APPLICATION="INSERT INTO \"restcomm_applications\" ("
        + "\"sid\", \"date_created\", \"date_updated\", \"friendly_name\", \"account_sid\","
        + " \"api_version\",\"voice_caller_id_lookup\", \"uri\", \"rcml_url\", \"kind\")"
        + " VALUES (#{sid}, #{date_created}, #{date_updated}, #{friendly_name}, #{account_sid},"
        + " #{api_version}, #{voice_caller_id_lookup}, #{uri}, #{rcml_url}, #{kind})";
    String DELETE_APPLICATION="DELETE FROM \"restcomm_applications\" WHERE \"sid\"=#{sid}";
    String DELETE_APPLICATIONS="DELETE FROM \"restcomm_applications\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_APPLICATION="UPDATE \"restcomm_applications\" SET \"friendly_name\"=#{friendly_name}, "
        + "\"date_updated\"=#{date_updated}, \"voice_caller_id_lookup\"=#{voice_caller_id_lookup},"
        + " \"rcml_url\"=#{rcml_url}, \"kind\"=#{kind} WHERE \"sid\"=#{sid}";

    @Insert(INSERT_APPLICATION)
    void addApplication(Map map);

    @Select(SELECT_APPLICATIN)
    Map<String,Object> getApplication(String sid);

    @Select(SELECT_APPLICATION_BY_FRIENDLY_NAME)
    Map<String,Object> getApplicationByFriendlyName(String friendlyName);

    @Select(SELECT_APPLICATINS)
    List<Map<String, Object>>  getApplications(String accountSid);

    @Delete(DELETE_APPLICATION)
    void removeApplication(String sid);

    @Delete(DELETE_APPLICATIONS)
    void removeApplications(String accountSid);

    @Update(UPDATE_APPLICATION)
    void updateApplication(Map map);
}
