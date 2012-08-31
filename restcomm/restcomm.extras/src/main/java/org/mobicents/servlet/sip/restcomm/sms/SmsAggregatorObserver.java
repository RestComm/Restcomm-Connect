package org.mobicents.servlet.sip.restcomm.sms;

public interface SmsAggregatorObserver {
  public void succeeded();
  public void failed();
}
