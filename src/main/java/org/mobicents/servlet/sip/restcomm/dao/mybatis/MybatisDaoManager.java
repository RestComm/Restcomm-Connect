/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountsDao;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.sip.restcomm.dao.AvailablePhoneNumbersDao;
import org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.sip.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.sip.restcomm.dao.OutgoingCallerIdsDao;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.sip.restcomm.dao.SandBoxesDao;
import org.mobicents.servlet.sip.restcomm.dao.ShortCodesDao;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MybatisDaoManager implements DaoManager {
  private Configuration configuration;
  private SqlSessionFactory sessions;
  private AccountsDao accountsDao;
  private ApplicationsDao applicationsDao;
  private AvailablePhoneNumbersDao availablePhoneNumbersDao;
  private CallDetailRecordsDao callDetailRecordsDao;
  private IncomingPhoneNumbersDao incomingPhoneNumbersDao;
  private NotificationsDao notificationsDao;
  private OutgoingCallerIdsDao outgoingCallerIdsDao;
  private RecordingsDao recordingsDao;
  private SandBoxesDao sandBoxesDao;
  private ShortCodesDao shortCodesDao;
  private SmsMessagesDao smsMessagesDao;
  private TranscriptionsDao transcriptionsDao;
  private GatewaysDao gatewaysDao;
  
  public MybatisDaoManager() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  @Override public AccountsDao getAccountsDao() {
    return accountsDao;
  }

  @Override public ApplicationsDao getApplicationsDao() {
    return applicationsDao;
  }

  @Override public AvailablePhoneNumbersDao getAvailablePhoneNumbersDao() {
    return availablePhoneNumbersDao;
  }

  @Override public CallDetailRecordsDao getCallDetailRecordsDao() {
    return callDetailRecordsDao;
  }

  @Override public IncomingPhoneNumbersDao getIncomingPhoneNumbersDao() {
    return incomingPhoneNumbersDao;
  }

  @Override public NotificationsDao getNotificationsDao() {
    return notificationsDao;
  }

  @Override public OutgoingCallerIdsDao getOutgoingCallerIdsDao() {
    return outgoingCallerIdsDao;
  }

  @Override public RecordingsDao getRecordingsDao() {
    return recordingsDao;
  }

  @Override public SandBoxesDao getSandBoxesDao() {
    return sandBoxesDao;
  }

  @Override public ShortCodesDao getShortCodesDao() {
    return shortCodesDao;
  }

  @Override public SmsMessagesDao getSmsMessagesDao() {
    return smsMessagesDao;
  }

  @Override public TranscriptionsDao getTranscriptionsDao() {
    return transcriptionsDao;
  }
  
  @Override public GatewaysDao getGatewaysDao() {
    return gatewaysDao;
  }
  
  @Override public void shutdown() {
    // Nothing to do.
  }

  @Override public void start() throws RuntimeException {
	// This must be called before any other MyBatis methods.
    org.apache.ibatis.logging.LogFactory.useLog4JLogging();
    // Load the configuration file.
    final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
    final String path = configuration.getString("configuration-file");
    Reader reader = null;
    try {
      reader = new FileReader(path);
    } catch(final FileNotFoundException exception) {
      throw new RuntimeException(exception);
    }
    final Properties properties = new Properties();
    final String sqlFiles = configuration.getString("sql-files");
    properties.setProperty("sql", sqlFiles);
    sessions = builder.build(reader, properties);
    // Instantiate the DAO objects.
    accountsDao = new MybatisAccountsDao(sessions);
    applicationsDao = new MybatisApplicationsDao(sessions);
    availablePhoneNumbersDao = new MybatisAvailablePhoneNumbersDao(sessions);
    callDetailRecordsDao = new MybatisCallDetailRecordsDao(sessions);
    incomingPhoneNumbersDao = new MybatisIncomingPhoneNumbersDao(sessions);
    notificationsDao = new MybatisNotificationsDao(sessions);
    outgoingCallerIdsDao = new MybatisOutgoingCallerIdsDao(sessions);
    recordingsDao = new MybatisRecordingsDao(sessions);
    sandBoxesDao = new MybatisSandBoxesDao(sessions);
    shortCodesDao = new MybatisShortCodesDao(sessions);
    smsMessagesDao = new MybatisSmsMessagesDao(sessions);
    transcriptionsDao = new MybatisTranscriptionsDao(sessions);
    gatewaysDao = new MybatisGatewaysDao(sessions);
  }
}
