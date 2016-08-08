package org.mobicents.servlet.restcomm.mappers;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordFilter;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
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
        String SELECT_CALL_DETAIL_RECORD_BY_INSTANCE_ID="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"instanceid\"=#{instanceid}";
        String SELECT_CALL_DETAIL_RECORD_BY_CONFERENCE_SID="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"conference_sid\"=#{conferenceSid}";
        String INSERT_CALL_DETAIL_RECORD="INSERT INTO \"restcomm_call_detail_records\" (\"sid\", \"instanceid\", \"parent_call_sid\", \"date_created\", \"date_updated\", \"account_sid\", \"recipient, sender\", \"phone_number_sid\", \"status\", \"start_time\", \"end_time\", \"duration\", \"price\", \"direction\", \"answered_by\", \"api_version\", \"forwarded_from\", \"caller_name\", \"uri\", \"call_path\", \"ring_duration\", \"conference_sid\", \"muted\", \"start_conference_on_enter\", \"end_conference_on_exit\", \"on_hold\") VALUES (#{sid}, #{instanceid}, #{parent_call_sid}, #{date_created}, #{date_updated}, #{account_sid}, #{to}, #{from}, #{phone_number_sid}, #{status}, #{start_time}, #{end_time}, #{duration}, #{price}, #{direction}, #{answered_by}, #{api_version}, #{forwarded_from}, #{caller_name}, #{uri}, #{call_path}, #{ring_duration}, #{conference_sid}, #{muted}, #{start_conference_on_enter}, #{end_conference_on_exit}, #{on_hold})";
        String DELETE_CALL_DETAIL="DELETE FROM \"restcomm_call_detail_records\" WHERE \"sid\"=#{sid}";
        String DELETE_CALL_DETAILS="DELETE FROM \"restcomm_call_detail_records\" WHERE \"account_sid\"=#{account_sid}";
        String UPDATE_CALL_DETAIL="UPDATE \"restcomm_call_detail_records\" SET \"date_updated\"=#{date_updated}, \"status\"=#{status}, \"start_time\"=#{start_time}, \"end_time\"=#{end_time}, \"duration\"=#{duration},\"price\"=#{price}, \"answered_by\"=#{answered_by}, \"ring_duration\"=#{ring_duration}, \"conference_sid\"=#{conference_sid}, \"muted\"=#{muted}, \"start_conference_on_enter\"=#{start_conference_on_enter}, \"end_conference_on_exit\"=#{end_conference_on_exit}, \"on_hold\"=#{on_hold} WHERE \"sid\"=#{sid}";
        String SELECT_TOTAL_CALL_DETAIL_RECORD_USING_FILTER="<script>"
            + "SELECT COUNT(*) FROM \"restcomm_call_detail_records\" WHERE \"account_sid\"=#{accountSid}"
            + "    <if test=\"instanceid != null\">"
            + "        AND \"instanceid\" like #{instanceid}"
            + "    </if>"
            + "    <if test=\"recipient != null\">"
            + "        AND \"recipient\" like #{recipient}"
            + "    </if>"
            + "    <if test=\"sender != null\">"
            + "        AND \"sender\" like #{sender}"
            + "    </if>"
            + "    <if test=\"status != null\">"
            + "        AND \"status\" like #{status}"
            + "    </if>"
            + "    <if test=\"parentCallSid != null\">"
            + "        AND \"parent_call_sid\" like #{parentCallSid}"
            + "    </if>"
            + "    <if test=\"startTime != null\">"
            + "        AND \"start_time\" >= #{startTime}"
            + "    </if>"
            + "    <if test=\"endTime != null\">"
            + "        AND \"end_time\" =< DATE_ADD(#{endTime},INTERVAL 1 DAY)"
            + "    </if>"
            + "</script>";
    String SELECT_CAAL_DETAIL_RECORD_USING_FILTER="<script>"
        + "    SELECT * FROM \"restcomm_call_detail_records\" AS restcomm_call_detail_records WHERE \"account_sid\"=#{accountSid}"
        + "    <if test=\"instanceid != null\">"
        + "        AND \"instanceid\" like #{instanceid}"
        + "    </if>"
        + "    <if test=\"recipient != null\">"
        + "        AND \"recipient\" like #{recipient}"
        + "    </if>"
        + "    <if test=\"sender != null\">"
        + "        AND \"sender\" like #{sender}"
        + "    </if>"
        + "    <if test=\"status != null\">"
        + "        AND \"status\" like #{status}"
        + "    </if>"
        + "    <if test=\"parentCallSid != null\">"
        + "        AND \"parent_call_sid\" like #{parentCallSid}"
        + "    </if>"
        + "    <if test=\"startTime != null\">"
        + "        AND \"start_time\" >= #{startTime}"
        + "    </if>"
        + "    <if test=\"endTime != null\">"
        + "        AND \"end_time\" =< DATE_ADD(#{endTime},INTERVAL 1 DAY) order by \"start_time\""
        + "    </if>"
        + "    LIMIT #{limit} OFFSET #{offset}"
        + "</script>";

        @Insert(INSERT_CALL_DETAIL_RECORD)
        void addCallDetailRecord(Map map);

        @Select(SELECT_CALL_DETAIL_RECORD)
        Map<String, Object> getCallDetailRecord(String sid);

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

        @Select(SELECT_CALL_DETAIL_RECORD_BY_INSTANCE_ID)
        List<Map<String, Object>> getCallDetailRecordsByInstanceId(String instanceid);

        @Select(SELECT_CALL_DETAIL_RECORD_BY_CONFERENCE_SID)
        List<Map<String, Object>> getCallDetailRecordsByConferenceSid(String conferenceSid);

        @Delete(DELETE_CALL_DETAIL)
        void removeCallDetailRecord(String sid);

        @Delete(DELETE_CALL_DETAILS)
        void removeCallDetailRecords(String accountSid);

        @Update(UPDATE_CALL_DETAIL)
        void updateCallDetailRecord(Map map);

        @Select(SELECT_TOTAL_CALL_DETAIL_RECORD_USING_FILTER)
        Integer getTotalCallDetailRecordByUsingFilters(CallDetailRecordFilter filter);

        @Select(SELECT_CAAL_DETAIL_RECORD_USING_FILTER)
        List<Map<String, Object>> getCallDetailRecordByUsingFilters(CallDetailRecordFilter filter);
}
