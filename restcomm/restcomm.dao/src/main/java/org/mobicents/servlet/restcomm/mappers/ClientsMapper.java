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
public interface ClientsMapper {

    String INSERT_CLIENT="INSERT INTO \"restcomm_clients\" (\"sid\", \"date_created\", \"date_updated\", "
        + "\"account_sid\", \"api_version\", \"friendly_name\", \"login\", \"password\", "
        + "\"status\", \"voice_url\", \"voice_method\", \"voice_fallback_url\", \"voice_fallback_method\", "
        + "\"voice_application_sid\", \"uri\") "
        + "VALUES ("
        + "#{sid}, "
        + "#{date_created}, "
        + "#{date_updated}, "
        + "#{account_sid}, "
        + "#{api_version}, "
        + "#{friendly_name}, "
        + "#{login}, "
        + "#{password}, "
        + "#{status}, "
        + "#{voice_url}, "
        + "#{voice_method}, "
        + "#{voice_fallback_url}, "
        + "#{voice_fallback_method}, "
        + "#{voice_application_sid}, "
        + "#{uri})";
    String UPDATE_CLIENT="UPDATE \"restcomm_clients\" SET "
        + "\"friendly_name\"=#{friendly_name}, "
        + "\"password\"=#{password}, "
        + "\"status\"=#{status}, "
        + "\"voice_url\"=#{voice_url}, "
        + "\"voice_method\"=#{voice_method}, "
        + "\"voice_fallback_url\"=#{voice_fallback_url}, "
        + "\"voice_fallback_method\"=#{voice_fallback_method}, "
        + "\"voice_application_sid\"=#{voice_application_sid} "
        + "WHERE \"sid\"=#{sid};";
    String SELECT_CLIENT="SELECT * FROM \"restcomm_clients\" WHERE \"sid\"=#{sid}";
    String SELECT_CLIENT_BY_LOGIN="SELECT * FROM \"restcomm_clients\" WHERE \"login\"=#{login}";
    String SELECT_CLIENTS_BY_ACCOUNT_SID="SELECT * FROM \"restcomm_clients\" WHERE \"account_sid\"=#{accountSid}";
    String SELECT_CLIENTS="SELECT * FROM \"restcomm_clients\"";
    String DELETE_CLIENT="DELETE FROM \"restcomm_clients\" WHERE \"sid\"=#{sid}";
    String DELETE_CLIENTS_BY_ACCOUNT_SID="DELETE FROM \"restcomm_clients\" WHERE \"account_sid\"=#{accountSid}";

    @Insert(INSERT_CLIENT)
    void addClient(Map map);

    @Update(UPDATE_CLIENT)
    void updateClient(Map map);

    @Select(SELECT_CLIENT)
    Map getClient(String sid);

    @Select(SELECT_CLIENT_BY_LOGIN)
    Map getClientByLogin(String login);

    @Select(SELECT_CLIENTS_BY_ACCOUNT_SID)
    List<Map<String, Object>> getClients(String accountSid);

    @Select(SELECT_CLIENTS)
    List<Map<String, Object>> getAllClients();

    @Delete(DELETE_CLIENT)
    void removeClient(String sid);

    @Delete(DELETE_CLIENTS_BY_ACCOUNT_SID)
    void removeClients(String accountSid);
}
