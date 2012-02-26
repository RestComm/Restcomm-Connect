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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.AvailablePhoneNumber;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AvailablePhoneNumbersDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoAvailablePhoneNumbersDao implements AvailablePhoneNumbersDao {
  private static final Logger logger = Logger.getLogger(MongoAvailablePhoneNumbersDao.class);
  private static final char[] lookupTable = new char[] {'2', '2', '2', '3', '3', '3', '4', '4', '4', '5', '5', '5',
      '6', '6', '6', '7', '7', '7', '7', '8', '8', '8', '9', '9', '9', '9'};
  private final DBCollection collection;

  public MongoAvailablePhoneNumbersDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_available_phone_numbers");
  }
  
  @Override public void addAvailablePhoneNumber(final AvailablePhoneNumber availablePhoneNumber) {
    final WriteResult result = collection.insert(toDbObject(availablePhoneNumber));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbers() {
    return getAvailablePhoneNumbers(null);
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByAreaCode(final String areaCode) {
	final BasicDBObject query = new BasicDBObject();
	query.put("iso_country", "US|CA");
	query.put("phone_number", "+1" + areaCode + "[0-9]{7,7}");
    return getAvailablePhoneNumbers(query);
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByPattern(final String pattern) {
	final BasicDBObject query = new BasicDBObject();
	query.put("phone_number", normalizePattern(pattern));
    return getAvailablePhoneNumbers(query);
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByRegion(final String region) {
    final BasicDBObject query = new BasicDBObject();
    query.put("region", region);
    return getAvailablePhoneNumbers(query);
  }

  @Override public List<AvailablePhoneNumber> getAvailablePhoneNumbersByPostalCode(final int postalCode) {
    final BasicDBObject query = new BasicDBObject();
    query.put("postal_code", postalCode);
    return getAvailablePhoneNumbers(query);
  }
  
  private List<AvailablePhoneNumber> getAvailablePhoneNumbers(final DBObject query) {
    final List<AvailablePhoneNumber> availablePhoneNumbers = new ArrayList<AvailablePhoneNumber>();
    DBCursor results = null;
    if(query != null) {
      results = collection.find(query);
    } else {
      results = collection.find();
    }
    while(results.hasNext()) {
      availablePhoneNumbers.add(toAvailablePhoneNumber(results.next()));
    }
    return availablePhoneNumbers;
  }
  
  private String normalizePattern(final String input) throws IllegalArgumentException {
    final char[] tokens = input.toUpperCase().toCharArray();
    final char[] result = new char[tokens.length];
    for(int index = 0; index < tokens.length; index++) {
      final char token = tokens[index];
      if(token == '+' || Character.isDigit(token)) {
        result[index] = token;
        continue;
      } else if(token == '*') { 
        result[index] = '.';
        continue;
      } else if(Character.isLetter(token)) {
    	final int delta = 65; // The decimal distance from 0x0000 to 0x0041 which equals to 'A'
        final int position = Character.getNumericValue(token) - delta;
        result[index] = lookupTable[position];
      } else {
        throw new IllegalArgumentException(token + " is not a valid character.");
      }
    }
    return new String(result);
  }

  @Override public void removeAvailablePhoneNumber(final String phoneNumber) {
    final BasicDBObject query = new BasicDBObject();
    query.put("phone_number", phoneNumber);
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private AvailablePhoneNumber toAvailablePhoneNumber(final DBObject object) {
    final String friendlyName = readString(object.get("friendly_name"));
    final String phoneNumber = readString(object.get("phone_number"));
    final Integer lata = readInteger(object.get("lata"));
    final String rateCenter = readString(object.get("rate_center"));
    final Double latitude = readDouble(object.get("latitude"));
    final Double longitude = readDouble(object.get("longitude"));
    final String region = readString(object.get("region"));
    final Integer postalCode = readInteger(object.get("postal_code"));
    final String isoCountry = readString(object.get("iso_country"));
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
