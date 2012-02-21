package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.AvailablePhoneNumber;

public interface AvailablePhoneNumbersDao {
  public void addAvailablePhoneNumber(AvailablePhoneNumber availablePhoneNumber);
  public List<AvailablePhoneNumber> getAvailablePhoneNumbers();
  public List<AvailablePhoneNumber> getAvailablePhoneNumbersByAreaCode(String areaCode);
  public List<AvailablePhoneNumber> getAvailablePhoneNumbersByPattern(String pattern);
  public List<AvailablePhoneNumber> getAvailablePhoneNumbersByRegion(String region);
  public List<AvailablePhoneNumber> getAvailablePhoneNumbersByPostalCode(int postalCode);
  public void removeAvailablePhoneNumber(String phoneNumber);
}
