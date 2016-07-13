package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface OutgoingCallerIdsMapper {

    String INSERT_OUTGOING_CALLER_ID="INSERT INTO \"restcomm_outgoing_caller_ids\" (\"sid\", \"date_created\", \"date_updated\", \"friendly_name\", \"account_sid\", \"phone_number\", \"uri\") VALUES (#{sid}, #{date_created}, #{date_updated}, #{friendly_name}, #{account_sid}, #{phone_number}, #{uri})";
    String SELECT_OUTGOING_CALLER_ID="SELECT * FROM \"restcomm_outgoing_caller_ids\" WHERE \"sid\"=#{sid}";
    String SELECT_OUTGOING_CALLER_IDS="SELECT * FROM \"restcomm_outgoing_caller_ids\" WHERE \"account_sid\"=#{accountSid}";
    String DELETE_OUTGOING_CALLER_ID="DELETE FROM \"restcomm_outgoing_caller_ids\" WHERE \"sid\"=#{sid}";
    String DELETE_OUTGOING_CALLER_IDS="DELETE FROM \"restcomm_outgoing_caller_ids\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_OUTGOING_CALLER_ID="UPDATE \"restcomm_outgoing_caller_ids\" SET \"date_updated\"=#{date_updated}, \"friendly_name\"=#{friendly_name} WHERE \"sid\"=#{sid}";

    @Insert(INSERT_OUTGOING_CALLER_ID)
    void addOutgoingCallerId(Map map);

    @Select(SELECT_OUTGOING_CALLER_ID)
    Map<String, Object> getOutgoingCallerId(String sid);

    @Select(SELECT_OUTGOING_CALLER_IDS)
    List<Map<String, Object>> getOutgoingCallerIds(String accountSid);

    @Delete(DELETE_OUTGOING_CALLER_ID)
    void removeOutgoingCallerId(String sid);

    @Delete(DELETE_OUTGOING_CALLER_IDS)
    void removeOutgoingCallerIds(String accountSid);

    @Update(UPDATE_OUTGOING_CALLER_ID)
    void updateOutgoingCallerId(Map map);
}
