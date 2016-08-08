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
public interface RecordingsMapper {

    String INSERT_RECORDING="INSERT INTO \"restcomm_recordings\" (\"sid\", \"date_created\", \"date_updated\", "
        + "\"account_sid\", \"call_sid\", \"duration\", \"api_version\", \"uri\", \"file_uri\") "
        + "VALUES "
        + "(#{sid}, #{date_created}, #{date_updated}, #{account_sid}, #{call_sid}, #{duration}, "
        + "#{api_version}, #{uri}, #{file_uri})";
    String SELECT_RECORDING="SELECT * FROM \"restcomm_recordings\" WHERE \"sid\"=#{sid}";
    String SELECT_RECORDINGS="SELECT * FROM \"restcomm_recordings\" WHERE \"account_sid\"=#{accountSid}";
    String SELECT_RECORDING_BY_CALL="SELECT * FROM \"restcomm_recordings\" WHERE \"call_sid\"=#{callSid} limit 1";
    String SELECT_RECORDINGS_BY_CALL="SELECT * FROM \"restcomm_recordings\" WHERE \"call_sid\"=#{callSid}";
    String DELETE_RECORDING="DELETE FROM \"restcomm_recordings\" WHERE \"sid\"=#{sid}";
    String DELETE_RECORDINGS="DELETE FROM \"restcomm_recordings\" WHERE \"account_sid\"=#{accountSid}";


    @Insert(INSERT_RECORDING)
    void addRecording(Map map);

    @Select(SELECT_RECORDING)
    Map<String, Object> getRecording(String sid);

    @Select(SELECT_RECORDINGS)
    List<Map<String, Object>> getRecordings(String accountSid);

    @Select(SELECT_RECORDING_BY_CALL)
    Map<String, Object> getRecordingByCall(String callSid);

    @Select(SELECT_RECORDINGS_BY_CALL)
    List<Map<String, Object>> getRecordingsByCall(String callSid);

    @Delete(DELETE_RECORDING)
    void removeRecording(String sid);

    @Delete(DELETE_RECORDINGS)
    void removeRecordings(String accountSid);
}
