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
public interface TranscriptionsMapper {

    String INSERT_TRANSCRIPTION="INSERT INTO \"restcomm_transcriptions\" (\"sid\", \"date_created\", \"date_updated\", \"account_sid\", \"status\", \"recording_sid\", \"duration\", \"transcription_text\", \"price\", \"uri\") VALUES (#{sid}, #{date_created}, #{date_updated}, #{account_sid}, #{status}, #{recording_sid}, #{duration}, #{transcription_text}, #{price}, #{uri})";
    String SELECT_TRANSCRIPTION="SELECT * FROM \"restcomm_transcriptions\" WHERE \"sid\"=#{sid}";
    String SELECT_TRANSCRIPTION_BY_RECORDING="SELECT * FROM \"restcomm_transcriptions\" WHERE \"recording_sid\"=#{recordingSid}";
    String SELECT_TRANSCRIPTIONS="SELECT * FROM \"restcomm_transcriptions\" WHERE \"account_sid\"=#{accountSid}";
    String DELETE_TRANSCRIPTION="DELETE FROM \"restcomm_transcriptions\" WHERE \"sid\"=#{sid}";
    String DELETE_TRANSCRIPTIONS="DELETE FROM \"restcomm_transcriptions\" WHERE \"account_sid\"=#{accountSid}";
    String UPDATE_TRANSCRIPTION="<script>"
        + "    UPDATE \"restcomm_transcriptions\" SET \"status\"=#{status}"
        + "    <if test=\"transcription_text != null\">"
        + "        , \"transcription_text\"=#{transcription_text} "
        + "    </if>"
        + "    WHERE \"sid\"=#{sid};"
        +"</script>";

    @Insert(INSERT_TRANSCRIPTION)
    void addTranscription(Map map);

    @Select(SELECT_TRANSCRIPTION)
    Map<String,Object> getTranscription(String sid);

    @Select(SELECT_TRANSCRIPTION_BY_RECORDING)
    Map<String,Object> getTranscriptionByRecording(String recordingSid);

    @Select(SELECT_TRANSCRIPTIONS)
    List<Map<String,Object>> getTranscriptions(String accountSid);

    @Delete(DELETE_TRANSCRIPTION)
    void removeTranscription(String sid);

    @Delete(DELETE_TRANSCRIPTIONS)
    void removeTranscriptions(String accountSid);

    @Update(UPDATE_TRANSCRIPTION)
    void updateTranscription(Map map);

}
