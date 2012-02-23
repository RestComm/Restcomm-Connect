package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.AvailablePhoneNumber;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AvailablePhoneNumbersDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoAvailablePhoneNumbersDao implements AvailablePhoneNumbersDao {
  private final DBCollection collection;

  public MongoAvailablePhoneNumbersDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_available_phone_numbers");
  }
  
  @Override public void addAvailablePhoneNumber(final AvailablePhoneNumber availablePhoneNumber) {
    
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbers() {
    return null;
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByAreaCode(final String areaCode) {
    return null;
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByPattern(final String pattern) {
    return null;
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByRegion(final String region) {
    return null;
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByPostalCode(final int postalCode) {
    return null;
  }

  @Override public void removeAvailablePhoneNumber(final String phoneNumber) {
    
  }
  
  private AvailablePhoneNumber toAvailablePhoneNumber(final DBObject object) {
    final String friendlyName = (String)object.get("friendly_name");
    final String phoneNumber = (String)object.get("phone_number");
    final Integer lata = (Integer)object.get("lata");
    final String rateCenter = (String)object.get("rate_center");
    final Double latitude = (Double)object.get("latitude");
    final Double longitude = (Double)object.get("longitude");
    final String region = (String)object.get("region");
    final Integer postalCode = (Integer)object.get("postal_code");
    final String isoCountry = (String)object.get("iso_country");
    return new AvailablePhoneNumber(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude,
        region, postalCode, isoCountry);
  }
  
  private DBObject toDbObject(final AvailablePhoneNumber availablePhoneNumber) {
	final BasicDBObject object = new BasicDBObject();
	object.put("friendly_name", availablePhoneNumber.getFriendlyName());
	object.put("phone_number", availablePhoneNumber.getPhoneNumber());
	object.put("lata", availablePhoneNumber.getLata());
	object.put("rate_center", availablePhoneNumber.getRateCenter());
	object.put("latitude", availablePhoneNumber.getLatitude());
	object.put("longitude", availablePhoneNumber.getLongitude());
	object.put("region", availablePhoneNumber.getRegion());
	object.put("postal_code", availablePhoneNumber.getPostalCode());
	object.put("iso_country", availablePhoneNumber.getIsoCountry());
    return object;
  }
}
