package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;

/**
 * @author brainslog@gmail.com (Alexandre Mendonca)
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface UsageMapper {

    String SELECT_DAILY_CALLS="SELECT"
       + "    'calls' AS \"category\","
       + "    \"account_sid\","
       + "    \"api_version\","
       + "    COUNT(1) AS \"count\","
       + "    CAST(${usageExprPre}${usageExprCol}${usageExprSuf} AS SIGNED) AS \"usage\","
       + "    SUM(CONVERT(\"price\", SIGNED)) AS \"price\","
       + "    CONVERT(MIN(date_created), DATE) AS \"start_date\","
       + "    CONVERT(MAX(date_created), DATE) AS \"end_date\","
       + "    '/todo' AS \"uri\""
       + "FROM"
       + "    \"${tableName}\""
       + "WHERE"
       + "    \"account_sid\"=#{sid} AND"
       + "    \"date_created\" >= #{startDate} AND"
       + "    \"date_created\" < DATE_ADD(#{endDate}, INTERVAL 1 DAY)"
       + "GROUP BY"
       + "    CAST(YEAR(date_created) AS CHAR) + '-' + CAST(MONTH(date_created) AS CHAR) + '-' + CAST(DAY(date_created) AS CHAR), account_sid, api_version"
       + "ORDER BY"
       + "    start_date";
    String SELECT_MONTHLY_CALLS="SELECT"
        + "    'calls' AS \"category\","
        + "    \"account_sid\","
        + "    \"api_version\","
        + "    COUNT(1) as \"count\","
        + "    CAST(${usageExprPre}${usageExprCol}${usageExprSuf} AS SIGNED) AS \"usage\""
        + "    SUM(CONVERT(\"price\", SIGNED)) AS \"price\""
        + "    CONVERT(MIN(date_created), DATE) AS \"start_date\","
        + "    CONVERT(MAX(date_created), DATE) AS \"end_date\","
        + "    '/todo' AS \"uri\""
        + "FROM"
        + "    ${tableName}"
        + "WHERE"
        + "    \"account_sid\"=#{sid} AND"
        + "    \"date_created\" >= #{startDate} AND"
        + "    \"date_created\" < DATE_ADD(#{endDate}, INTERVAL 1 DAY)"
        + "GROUP BY"
        + "    CAST(YEAR(date_created) AS CHAR) + '-' + CAST(MONTH(date_created) AS CHAR), account_sid, api_version"
        + "ORDER BY"
        + "    \"start_date\"";
    String SELECT_YEARLY_CALLS="SELECT"
        + "    'calls' AS \"category\","
        + "    \"account_sid\","
        + "    \"api_version\","
        + "    COUNT(1) as \"count\","
        + "    CAST(${usageExprPre}${usageExprCol}${usageExprSuf} AS SIGNED) AS \"usage\","
        + "    SUM(CONVERT(\"price\", SIGNED)) AS \"price\","
        + "    CONVERT(MIN(date_created), DATE) AS \"start_date\","
        + "    CONVERT(MAX(date_created), DATE) AS \"end_date\","
        + "    '/todo' AS \"uri\""
        + "FROM"
        + "    ${tableName}"
        + "WHERE"
        + "    \"account_sid\"=#{sid} AND"
        + "    \"date_created\" >= #{startDate} AND"
        + "    \"date_created\" < DATE_ADD(#{endDate}, INTERVAL 1 DAY)"
        + "GROUP BY"
        + "    CAST(YEAR(date_created) AS CHAR), \"account_sid\", \"api_version\""
        + "ORDER BY"
        + "    \"start_date\"";
    String SELECT_ALL_TIME="SELECT"
        + "    'calls' AS \"category\","
        + "    \"account_sid\","
        + "    \"api_version\","
        + "    COUNT(1) as \"count\","
        + "    CAST(${usageExprPre}${usageExprCol}${usageExprSuf} AS SIGNED) AS \"usage\","
        + "    SUM(CONVERT(\"price\", SIGNED)) AS \"price\","
        + "    CONVERT(MIN(date_created), DATE) AS \"start_date\","
        + "    CONVERT(MAX(date_created), DATE) AS \"end_date\","
        + "    '/todo' AS \"uri\""
        + "FROM"
        + "    ${tableName}"
        + "WHERE"
        + "    \"account_sid\"=#{sid} AND"
        + "    \"date_created\" >= #{startDate} AND"
        + "    \"date_created\" < DATE_ADD(#{endDate}, INTERVAL 1 DAY)"
        + "GROUP BY"
        + "    EXTRACT(MONTH FROM \"date_created\"), \"account_sid\", \"api_version\""
        + "ORDER BY"
        + "    \"start_date\"";

    @Select(SELECT_DAILY_CALLS)
    List<Map<String,Object>> getDailyCalls(Map map);

    @Select(SELECT_MONTHLY_CALLS)
    List<Map<String,Object>> getMonthlyCalls(Map map);

    @Select(SELECT_YEARLY_CALLS)
    List<Map<String,Object>> getYearlyCalls(Map map);

    @Select(SELECT_ALL_TIME)
    List<Map<String,Object>> getAllTimeCalls(Map map);

}
