package org.mobicents.servlet.restcomm.mappers;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface CallDetailRecordMapper {

        String SELECT_CALL_DETAIL_RECORD="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"sid\"=#{sid}";
        String SELECT_CALL_DETAIL_RECORDS="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"account_sid\"=#{accountSid}";
        String SELECT_CALL_DETAIL_RECORD_BY_RECIPIENT="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"recipient\"=#{to}";
        String SELECT_CALL_DETAIL_RECORD_BY_SENDER="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"sender\"=#{from}";
        String SELECT_CALL_DETAIL_RECORD_BY_STATUS="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"status\"=#{status}";
        String SELECT_CALL_DETAIL_RECORD_BY_START_TIME="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"start_time\"&gt;=#{startTime} AND \"start_time\"&lt;DATE_ADD(#{startTime},INTERVAL 1 DAY)";
        String SELECT_CALL_DETAIL_RECORD_BY_END_TIME="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"end_time\"&gt;=#{endTime} AND \"end_time\"&lt;DATE_ADD(#{endTime},INTERVAL 1 DAY)";
        String SELECT_CALL_DETAIL_RECORD_BY_START_AND_END_TIME="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"start_time\"&gt;=#{startTime} AND \"end_time\"&lt;=#{endTime}";
        String SELECT_CALL_DETAIL_RECORD_BY_PARENT_CALL="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"parent_call_sid\"=#{parentCallSid}";
        String INSERT_CALL_DETAIL_RECORD="INSERT INTO \"restcomm_call_detail_records\" (\"sid\", \"parent_call_sid\", \"date_created\", "
            + "\"date_updated\", \"account_sid\", \"recipient\", \"sender\", \"phone_number_sid\", "
            + "\"status\", \"start_time\", \"end_time\", \"duration\", \"price\", \"direction\", "
            + "\"answered_by\", \"api_version\", \"forwarded_from\", \"caller_name\", \"uri\", "
            + "\"call_path\", \"ring_duration\") "
            + "VALUES ("
            + "#{sid}, #{parent_call_sid}, #{date_created}, #{date_updated}, #{account_sid}, #{to}, "
            + "#{from}, #{phone_number_sid}, #{status}, #{start_time}, #{end_time}, #{duration}, "
            + "#{price}, #{direction}, #{answered_by}, #{api_version}, #{forwarded_from}, "
            + "#{caller_name}, #{uri}, #{call_path}, #{ring_duration}"
            + ")";
        String DELETE_CALL_DETAIL="DELETE FROM \"restcomm_call_detail_records\" WHERE \"sid\"=#{sid}";
        String DELETE_CALL_DETAILS="DELETE FROM \"restcomm_call_detail_records\" WHERE \"account_sid\"=#{account_sid}";
        String UPDATE_CALL_DETAIL="UPDATE \"restcomm_call_detail_records\" SET \"date_updated\"=#{date_updated}, \"status\"=#{status}, "
        		+ "\"start_time\"=#{start_time}, \"end_time\"=#{end_time}, \"duration\"=#{duration}, \"price\"=#{price}, \"answered_by\"=#{answered_by}, "
        		+ "\"ring_duration\"=#{ring_duration} WHERE \"sid\"=#{sid}";

        @Insert(INSERT_CALL_DETAIL_RECORD)
        void addCallDetailRecord(Map map);

        @Select(SELECT_CALL_DETAIL_RECORD)
        Map getCallDetailRecord(String sid);

        @Select(SELECT_CALL_DETAIL_RECORDS)
        List<Map<String, Object>> getCallDetailRecords(String accountSid);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_RECIPIENT)
        List<Map<String, Object>> getCallDetailRecordsByRecipient(String to);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_SENDER)
        List<Map<String, Object>> getCallDetailRecordsBySender(String from);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_STATUS)
        List<Map<String, Object>> getCallDetailRecordsByStatus(String status);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_START_TIME)
        List<Map<String, Object>> getCallDetailRecordsByStartTime(Date startTime);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_END_TIME)
        List<Map<String, Object>> getCallDetailRecordsByEndTime(Date endTime);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_START_AND_END_TIME)
        List<Map<String, Object>> getCallDetailRecordsByStarTimeAndEndTime(Date startTime,Date endTime);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_PARENT_CALL)
        List<Map<String, Object>> getCallDetailRecordsByParentCall(String parentCallSid);

        @Delete(DELETE_CALL_DETAIL)
        void removeCallDetailRecord(String sid);

        @Delete(DELETE_CALL_DETAILS)
        void removeCallDetailRecords(String accountSid);

        @Update(UPDATE_CALL_DETAIL)
        void updateCallDetailRecord(Map map);

        
        Integer getTotalCallDetailRecordByUsingFilters();
}
