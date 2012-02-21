package org.mobicents.servlet.sip.restcomm.dao;

public interface DaoManager {
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
}
