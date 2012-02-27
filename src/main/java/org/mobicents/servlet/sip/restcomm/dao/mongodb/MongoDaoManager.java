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
package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.UnknownHostException;

import org.apache.commons.configuration.Configuration;

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

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoDaoManager implements DaoManager {
  private Configuration configuration;
  private Mongo mongo;
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

  public MongoDaoManager() {
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

  @Override	public OutgoingCallerIdsDao getOutgoingCallerIdsDao() {
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

  @Override public void start() throws RuntimeException {
    final String host = configuration.getString("host");
    final Integer port = configuration.getInt("port");
    final String db = configuration.getString("database");
    final String user = configuration.getString("user");
    final String password = configuration.getString("password");
    // Connect to the MongoDB server.
    DB database = null;
    try {
	  mongo = new Mongo(host, port);
	  database = mongo.getDB(db);
	  if(user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
	    if(!database.authenticate(user, password.toCharArray())) {
	      throw new RuntimeException("Authentication for " + user + " failed.");
	    }
	  }
	} catch(final UnknownHostException exception) {
	  throw new RuntimeException(exception);
	}
    // Instantiate the DAO objects.
    accountsDao = new MongoAccountsDao(database);
    applicationsDao = new MongoApplicationsDao(database);
    availablePhoneNumbersDao = new MongoAvailablePhoneNumbersDao(database);
    callDetailRecordsDao = new MongoCallDetailRecordsDao(database);
    incomingPhoneNumbersDao = new MongoIncomingPhoneNumbersDao(database);
    notificationsDao = new MongoNotificationsDao(database);
    outgoingCallerIdsDao = new MongoOutgoingCallerIdsDao(database);
    recordingsDao = new MongoRecordingsDao(database);
    sandBoxesDao = new MongoSandBoxesDao(database);
    shortCodesDao = new MongoShortCodesDao(database);
    smsMessagesDao = new MongoSmsMessagesDao(database);
    transcriptionsDao = new MongoTranscriptionsDao(database);
    gatewaysDao = new MongoGatewaysDao(database);
  }

  @Override public void shutdown() {
    mongo.close();
  }
}
