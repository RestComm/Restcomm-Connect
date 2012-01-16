package org.mobicents.servlet.sip.restcomm;

public interface AvailablePhoneNumber {
  public String getFriendlyName();
  public String getPhoneNumber();
  public Integer getLata();
  public String getRateCenter();
  public Double getLatitude();
  public Double getLongitude();
  public String getRegion();
  public Integer getPostalCode();
  public String getIsoCountry();
}
