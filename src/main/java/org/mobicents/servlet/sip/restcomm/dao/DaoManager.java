package org.mobicents.servlet.sip.restcomm.dao;

import org.mobicents.servlet.sip.restcomm.Configurable;
import org.mobicents.servlet.sip.restcomm.LifeCycle;

public interface DaoManager extends Configurable, LifeCycle{
  public AccountsDao getAccountsDao();
  public ApplicationsDao getApplicationsDao();
  public AvailablePhoneNumbersDao getAvailablePhoneNumbersDao();
  public CallDetailRecordsDao getCallDetailRecordsDao();
  public IncomingPhoneNumbersDao getIncomingPhoneNumbersDao();
  public NotificationsDao getNotificationsDao();
  public OutgoingCallerIdsDao getOutgoingCallerIdsDao();
  public RecordingsDao getRecordingsDao();
  public SandBoxesDao getSandBoxesDao();
  public ShortCodesDao getShortCodesDao();
  public SmsMessagesDao getSmsMessagesDao();
  public TranscriptionsDao getTranscriptionsDao();
  public GatewaysDao getGatewaysDao();
}
