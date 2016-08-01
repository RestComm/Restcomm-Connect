package org.mobicents.servlet.restcomm.mappers;

import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface InstanceIdMapper {

    String INSERT_INSTANCEID="INSERT INTO \"restcomm_instance_id\" (\"instance_id\", \"host\", \"date_created\", \"date_updated\") VALUES(#{instance_id}, #{host},  #{date_created}, #{date_updated})";
    String SELECT_INSTANCEID="SELECT * FROM \"restcomm_instance_id\"";
    String SELECT_INSTANCEID_BY_HOST="SELECT * FROM \"restcomm_instance_id\" where \"host\"=#{host}";
    String UPDATE_INSTANCEID="UPDATE \"restcomm_instance_id\" SET \"date_updated\"=#{date_updated}, \"instance_id\"=#{instance_id} WHERE \"instance_id\"=#{instance_id}";
    String DELETE_INSTANCEID="DELETE FROM \"restcomm_instance_id\" WHERE \"instance_id\"=#{instance_id}";

    @Insert(INSERT_INSTANCEID)
    void addInstanceId(Map map);

    @Select(SELECT_INSTANCEID)
    Map<String, Object> getInstanceId();

    @Select(SELECT_INSTANCEID_BY_HOST)
    Map<String, Object> getInstanceIdByHost(String host);

    @Update(UPDATE_INSTANCEID)
    void updateInstanceId(Map map);

    @Delete(DELETE_INSTANCEID)
    void removeInstanceId(String instance_id);
}
