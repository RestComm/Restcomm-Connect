package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public interface HttpCookiesMapper {

    String INSERT_COOKIES="INSERT INTO \"restcomm_http_cookies\" (\"sid\", \"comment\", \"domain\", "
        + "\"expiration_date\", \"name\", \"path\", \"value\", \"version\") "
        + "VALUES ("
        + "#{sid}, "
        + "#{comment}, "
        + "#{domain}, "
        + "#{expiration_date}, "
        + "#{name}, "
        + "#{path}, "
        + "#{value}, "
        + "#{version})";
    String SELECT_COOKIES="SELECT * FROM \"restcomm_http_cookies\"";
    String HAS_COOKIES="SELECT COUNT(*) FROM \"restcomm_http_cookies\" WHERE \"sid\"=#{sid} AND \"name\"=#{name}";
    String HAS_EXPIRED_COOKIES="SELECT COUNT(*) FROM \"restcomm_http_cookies\" WHERE \"sid\"=#{sid} AND \"expiration_date\" &lt;= NOW()";
    String DELETE_COOKIES="DELETE FROM \"restcomm_http_cookies\" WHERE \"sid\"=#{sid}";
    String DELETE_EXPIRED_COOKIES="DELETE FROM \"restcomm_http_cookies\" WHERE \"sid\"=#{sid} AND \"expiration_date\" &lt;= NOW()";
    String UPDATE_COOKIES="UPDATE \"restcomm_http_cookies\" SET \"comment\"=#{comment}, \"expiration_date\"=#{expiration_date}, \"value\"=#{value} WHERE \"sid\"=#{sid} AND \"name\"=#{name}";

    @Insert(INSERT_COOKIES)
    void addCookie(Map map);

    @Select(SELECT_COOKIES)
    List<Map<String, Object>> getCookies();

    @Select(HAS_COOKIES)
    Integer hasCookie(Map map);

    @Select(HAS_EXPIRED_COOKIES)
    Integer hasExpiredCookies(String sid);

    @Delete(DELETE_COOKIES)
    void removeCookies(String sid);

    @Delete(DELETE_EXPIRED_COOKIES)
    void removeExpiredCookies(String sid);

    @Update(UPDATE_COOKIES)
    void updateCookie(Map map);
}
