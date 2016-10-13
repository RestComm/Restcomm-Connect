package org.mobicents.servlet.restcomm.mappers;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;

/**
 * @author maria-farooq@live.com (Maria Farooq
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface ConferenceDetailRecordsMapper {

    String INSERT_CONFERENCE_DETAIL_RECORD="INSERT INTO \"restcomm_conference_detail_records\" (\"sid\", \"date_created\", \"date_updated\", \"account_sid\", \"status\", \"friendly_name\", \"api_version\", \"uri\") VALUES (#{sid}, #{date_created}, #{date_updated}, #{account_sid}, #{status}, #{friendly_name}, #{api_version}, #{uri})";
    String SELECT_CONFERENCE_DETAIL_RECORD="SELECT * FROM \"restcomm_conference_detail_records\" WHERE \"sid\"=#{sid}";
    String SELECT_CONFERENCE_DETAIL_RECORDS="SELECT * FROM \"restcomm_conference_detail_records\" WHERE \"account_sid\"=#{accountSid}";
    String SELECT_CONFERENCE_DETAIL_RECORDS_BY_STATUS="SELECT * FROM \"restcomm_conference_detail_records\" WHERE \"status\"=#{status}";
    String SELECT_CONFERENCE_DETAIL_RECORDS_DATE_CREATED="SELECT * FROM \"restcomm_conference_detail_records\" WHERE \"date_created\">=#{dateCreated} AND \"date_created\"<=DATE_ADD(#{dateCreated},INTERVAL 1 DAY)";
    String SELECT_CONFERENCE_DETAIL_RECORDS_DATE_UPDATED="SELECT * FROM \"restcomm_conference_detail_records\" WHERE \"date_updated\">=#{dateUpdated} AND \"date_updated\"<=DATE_ADD(#{dateUpdated},INTERVAL 1 DAY)";
    String SELECT_TOTAL_CONFERENCE_DETAIL_RECORDS_DATE_BY_USING_FILTER="<script>"
        + "SELECT COUNT(*) FROM \"restcomm_conference_detail_records\" WHERE \"account_sid\"=#{accountSid}"
        + "<if test=\"status != null\">"
        + " AND \"status\" like #{status} "
        + "</if>"
        + "<if test=\"friendlyName != null\"> "
        + " AND \"friendly_name\" like #{friendlyName} "
        + "</if>"
        + "<if test=\"dateCreated != null\">"
        + "   AND \"date_created\" &gt;= #{dateCreated}"
        + "</if>"
        + "<if test=\"dateUpdated != null\">"
        + " AND \"date_updated\" &lt;= DATE_ADD(#{dateUpdated},INTERVAL 1 DAY) order by \"date_created\""
        + "</if>"
        + "</script>";
    String SELECT_CONFERENCE_DETAIL_RECORDS_DATE_BY_USING_FILTER="<script>"
        + " SELECT * FROM \"restcomm_conference_detail_records\" AS restcomm_conference_detail_records WHERE \"account_sid\"=#{accountSid}"
        + " <if test=\"status != null\">"
        + " AND \"status\" like #{status}"
        + " </if>"
        + " <if test=\"friendlyName != null\">"
        + " AND \"friendly_name\" like #{friendlyName}"
        + " </if>"
        + " <if test=\"dateCreated != null\">"
        + " AND \"date_created\" &gt;= #{dateCreated}"
        + " </if>"
        + " <if test=\"dateUpdated != null\">"
        + " AND \"date_updated\" &lt;= DATE_ADD(#{dateUpdated},INTERVAL 1 DAY) order by \"date_created\""
        + " </if>"
        + " LIMIT #{limit} OFFSET #{offset}"
        + " </script>";
    String DELETE_CONFERENCE_DETAIL_RECORD="DELETE FROM \"restcomm_conference_detail_records\" WHERE \"sid\"=#{sid}";
    String DELETE_CONFERENCE_DETAIL_RECORDS="DELETE FROM \"restcomm_conference_detail_records\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_CONFERENCE_DETAIL_RECORD="UPDATE \"restcomm_conference_detail_records\" SET \"date_updated\"=#{date_updated}, \"status\"=#{status} WHERE \"sid\"=#{sid}";

    @Insert(INSERT_CONFERENCE_DETAIL_RECORD)
    void addConferenceDetailRecord(Map map);

    @Select(SELECT_CONFERENCE_DETAIL_RECORD)
    Map<String,Object> getConferenceDetailRecord(String sid);

    @Select(DELETE_CONFERENCE_DETAIL_RECORDS)
    List<Map<String,Object>> getConferenceDetailRecords(String accountSid);

    @Select(SELECT_CONFERENCE_DETAIL_RECORDS_BY_STATUS)
    List<Map<String,Object>> getConferenceDetailRecordsByStatus(String status);

    @Select(SELECT_CONFERENCE_DETAIL_RECORDS_DATE_CREATED)
    List<Map<String,Object>> getConferenceDetailRecordsByDateCreated(Date dateCreated);

    @Select(SELECT_CONFERENCE_DETAIL_RECORDS_DATE_UPDATED)
    List<Map<String,Object>> getConferenceDetailRecordsByDateUpdated(Date dateUpdated);

    @Select(SELECT_TOTAL_CONFERENCE_DETAIL_RECORDS_DATE_BY_USING_FILTER)
    Integer getTotalConferenceDetailRecordByUsingFilters(ConferenceDetailRecordFilter filter);

    @Select(SELECT_CONFERENCE_DETAIL_RECORDS_DATE_BY_USING_FILTER)
    List<Map<String,Object>> getConferenceDetailRecordByUsingFilters(ConferenceDetailRecordFilter filter);

    @Delete(DELETE_CONFERENCE_DETAIL_RECORD)
    void removeConferenceDetailRecord(String sid);

    @Delete(DELETE_CONFERENCE_DETAIL_RECORDS)
    void removeConferenceDetailRecords(String accountSid);

    @Update(UPDATE_CONFERENCE_DETAIL_RECORD)
    void updateConferenceDetailRecord(Map map);

}
