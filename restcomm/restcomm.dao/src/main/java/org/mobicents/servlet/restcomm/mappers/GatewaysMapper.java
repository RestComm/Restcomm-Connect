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
public interface GatewaysMapper {

    String INSERT_GATEWAY="INSERT INTO \"restcomm_gateways\" (\"sid\", \"date_created\", \"date_updated\", "
        + "\"friendly_name\", \"password\", \"proxy\", \"register\", \"user_name\", "
        + "\"ttl\", \"uri\") "
        + "VALUES ("
        + "#{sid}, "
        + "#{date_created}, "
        + "#{date_updated}, "
        + "#{friendly_name}, "
        + "#{password}, "
        + "#{proxy}, "
        + "#{register}, "
        + "#{user_name}, "
        + "#{ttl}, "
        + "#{uri})";
    String SELECT_GATEWAY="SELECT * FROM \"restcomm_gateways\" WHERE \"sid\"=#{sid}";
    String SELECT_GATWAYS="SELECT * FROM \"restcomm_gateways\"";
    String DELETE_GATEWAY="DELETE FROM \"restcomm_gateways\" WHERE \"sid\"=#{sid}";
    String UPCATE_GATEWAY="UPDATE \"restcomm_gateways\" SET "
        + "\"date_updated\"=#{date_updated}, "
        + "\"friendly_name\"=#{friendly_name}, "
        + "\"password\"=#{password}, "
        + "\"proxy\"=#{proxy}, "
        + "\"register\"=#{register}, "
        + "\"user_name\"=#{user_name}, "
        + "\"ttl\"=#{ttl} "
        + "WHERE \"sid\"=#{sid}";

    @Insert(INSERT_GATEWAY)
    void addGateway(Map map);

    @Select(SELECT_GATEWAY)
    Map getGateway(String sid);

    @Select(SELECT_GATWAYS)
    List<Map<String, Object>> getGateways();

    @Delete(DELETE_GATEWAY)
    void removeGateway(String sid);

    @Update(UPCATE_GATEWAY)
    void updateGateway(Map map);
}
