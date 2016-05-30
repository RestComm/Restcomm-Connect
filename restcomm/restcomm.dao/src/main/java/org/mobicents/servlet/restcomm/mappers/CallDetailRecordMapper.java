package org.mobicents.servlet.restcomm.mappers;

public interface CallDetailRecordMapper {

        String SELECT_CALL_DETAIL_RECORD="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"sid\"=#{sid}";
        String SELECT_CALL_DETAIL_RECORDS="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"account_sid\"=#{account_sid}";
        String SELECT_CALL_DETAIL_RECORD_BY_RECIPIENT="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"recipient\"=#{to}";
        String SELECT_CALL_DETAIL_BY_SENDER="SELECT * FROM \"restcomm_call_detail_records\" WHERE \"sender\"=#{from}";
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
}
