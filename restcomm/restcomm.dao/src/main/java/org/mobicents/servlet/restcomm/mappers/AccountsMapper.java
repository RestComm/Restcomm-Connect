package org.mobicents.servlet.restcomm.mappers;

import java.util.List;
import java.util.Map;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author zahid.med@gmail.com (ZAHID Mohammed)
 */
public interface AccountsMapper {

    String SELECT_ACOUNT="SELECT * FROM \"restcomm_accounts\" WHERE \"sid\"=#{sid}";
    String SELECT_ACCOUNT_BY_FREINDLY_NAME="SELECT * FROM \"restcomm_accounts\" WHERE \"friendly_name\"=#{name}";
    String SELECT_ACCOUNT_BY_EMAIL="SELECT * FROM \"restcomm_accounts\" WHERE \"email_address\"=#{email}";
    String SELECT_ACCOUNT_BY_ACCOUNT_SID="SELECT * FROM \"restcomm_accounts\" WHERE \"account_sid\"=#{accountSid}";
    String INSERT_ACCOUNT="INSERT INTO \"restcomm_accounts\" (\"sid\", \"date_created\", \"date_updated\","
    + "\"email_address\", \"friendly_name\", \"account_sid\", \"type\", \"status\", \"auth_token\",\"role\", \"uri\")"
    + "VALUES("
    + " #{sid},"
    + " #{date_created},"
    + " #{date_updated},"
    + " #{email_address},"
    + " #{friendly_name},"
    + " #{account_sid},"
    + " #{type},"
    + " #{status},"
    + " #{auth_token},"
    + " #{role},"
    + " #{uri}"
    + ")";
    String DELETE_ACCOUNT="DELETE FROM \"restcomm_accounts\" WHERE \"sid\"=#{sid}";
    String UPDATE_ACCOUNT="UPDATE \"restcomm_accounts\" SET "
    + " \"date_updated\"=#{date_updated},"
    + " \"email_address\"=#{email_address},"
    + " \"friendly_name\"=#{friendly_name},"
    + " \"type\"=#{type},"
    + " \"status\"=#{status},"
    + " \"auth_token\"=#{auth_token},"
    + " \"role\"=#{role}"
    + " WHERE"
    + " \"sid\"=#{sid}";

    @Select(SELECT_ACOUNT)
    Map<String,Object> getAccount(String sid);

    @Select(SELECT_ACCOUNT_BY_FREINDLY_NAME)
    Map<String,Object> getAccountByFriendlyName(String name);

    @Select(SELECT_ACCOUNT_BY_EMAIL)
    Map<String,Object> getAccountByEmail(String email);

    @Select(SELECT_ACCOUNT_BY_ACCOUNT_SID)
    List<Map<String, Object>>  getAccounts(String accountSid);

    @Insert(INSERT_ACCOUNT)
    void addAccount(Map map);

    @Delete(DELETE_ACCOUNT)
    void removeAccount(String sid);

    @Update(UPDATE_ACCOUNT)
    void updateAccount(Map map);
}
