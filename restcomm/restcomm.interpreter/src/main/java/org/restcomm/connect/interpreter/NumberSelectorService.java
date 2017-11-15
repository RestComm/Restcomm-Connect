/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.interpreter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberFilter;

public class NumberSelectorService {

    private static Logger logger = Logger.getLogger(NumberSelectorService.class);

    private IncomingPhoneNumbersDao numbersDao;

    public NumberSelectorService(IncomingPhoneNumbersDao numbersDao) {
        this.numbersDao = numbersDao;
    }

    /**
     *
     * @param phone
     * @return a list of strings to match number based on different formats
     */
    private List<String> createPhoneQuery(String phone) {
        List<String> numberQueries = new ArrayList<String>(10);
        numberQueries.add(phone);
        // Format the destination to an E.164 phone number.
        if (!(phone.contains("*") || phone.contains("#"))) {
            try {
                final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
                String usFormatNum = phoneNumberUtil.format(phoneNumberUtil.parse(phone, "US"), PhoneNumberUtil.PhoneNumberFormat.E164);
                if (!numberQueries.contains(usFormatNum)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding US Format num to queries:" + usFormatNum);
                    }
                    numberQueries.add(usFormatNum);
                }
            } catch (NumberParseException e) {
                //logger.error("Exception when try to format : " + e);
            }
        }
        if (phone.startsWith("+")) {
            //remove the (+) and check if exists
            String noPlusNum = phone.replaceFirst("\\+", "");
            if (!numberQueries.contains(noPlusNum)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding No Plus Num:" + noPlusNum);
                }
                numberQueries.add(noPlusNum);
            }
        } else {
            String plusNum = "+".concat(phone);
            if (!numberQueries.contains(plusNum)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding Plus Num:" + plusNum);
                }
                numberQueries.add(plusNum);
            }
        }
        return numberQueries;
    }

    private IncomingPhoneNumber findSingleNumber(String number,
            Sid sourceOrganizationSid, Sid destinationOrganizationSid) {
        IncomingPhoneNumber matchedNumber = null;
        IncomingPhoneNumberFilter.Builder filterBuilder = IncomingPhoneNumberFilter.Builder.builder();
        filterBuilder.byPhoneNumber(number);
        if (destinationOrganizationSid != null) {
            filterBuilder.byOrgSid(destinationOrganizationSid.toString());
        }
        if (sourceOrganizationSid!= null &&
                !sourceOrganizationSid.equals(destinationOrganizationSid)) {
            filterBuilder.byPureSIP(Boolean.FALSE);
        }
        IncomingPhoneNumberFilter numFilter = filterBuilder.build();
        if (logger.isDebugEnabled()) {
            logger.debug("Searching with filter:" + numFilter);
        }
        List<IncomingPhoneNumber> matchedNumbers = numbersDao.getIncomingPhoneNumbersByFilter(numFilter);
        if (logger.isDebugEnabled()) {
            logger.debug("Num of results:" + matchedNumbers.size());
        }
        //we expect a perfect match, so first result taken
        if (matchedNumbers != null && matchedNumbers.size() > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Matched number with filter:" + matchedNumbers.get(0));
            }
            matchedNumber = matchedNumbers.get(0);
        }
        return matchedNumber;
    }

    private IncomingPhoneNumber findByNumber(List<String> numberQueries,
            Sid sourceOrganizationSid, Sid destinationOrganizationSid) {
        IncomingPhoneNumber matchedNumber = null;

        int i = 0;
        while (matchedNumber == null && i < numberQueries.size()) {
            matchedNumber = findSingleNumber(numberQueries.get(i),
                    sourceOrganizationSid, destinationOrganizationSid);
            i = i + 1;
        }
        return matchedNumber;
    }

    public IncomingPhoneNumber searchNumber(String phone,
            Sid sourceOrganizationSid, Sid destinationOrganizationSid) {
        if (logger.isDebugEnabled()) {
            logger.debug("getMostOptimalIncomingPhoneNumber: " + phone
                    + ",srcOrg:" + sourceOrganizationSid
                    + ",destOrg:" + destinationOrganizationSid);
        }
        List<String> numberQueries = createPhoneQuery(phone);

        IncomingPhoneNumber numberfound = findByNumber(numberQueries, sourceOrganizationSid, destinationOrganizationSid);
        if (numberfound == null) {
            //if no perfect match found, try with special star number
            numberfound = findSingleNumber("*", sourceOrganizationSid, destinationOrganizationSid);
            if (numberfound == null) {
                //only use regex if perfect match didnt worked
                if (destinationOrganizationSid != null
                        && phone.matches("[\\d,*,#,+]+")) {
                    //only use regex if org available
                    //check if there is a Regex match only if parameter is a String aka phone Number
                    numberfound = findByRegex(numberQueries, sourceOrganizationSid, destinationOrganizationSid);
                }
            }
        }
        return numberfound;
    }

    class NumberLengthComparator implements Comparator<IncomingPhoneNumber> {

        @Override
        public int compare(IncomingPhoneNumber o1, IncomingPhoneNumber o2) {
            //put o2 first to make longest first in coll
            return Integer.compare(o2.getPhoneNumber().length(), o1.getPhoneNumber().length());
        }

    }

    /**
     * @param numbers
     * @param phone
     * @param numbersDao
     * @return
     */
    private IncomingPhoneNumber findByRegex(List<String> numberQueries,
            Sid sourceOrganizationSid, Sid destOrg) {
        IncomingPhoneNumber numberFound = null;
        IncomingPhoneNumberFilter.Builder filterBuilder = IncomingPhoneNumberFilter.Builder.builder();
        filterBuilder.byOrgSid(destOrg.toString());
        filterBuilder.byPureSIP(Boolean.FALSE);
        List<IncomingPhoneNumber> regexList = numbersDao.getIncomingPhoneNumbersRegex(filterBuilder.build());
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Found %d Regex IncomingPhone numbers.", regexList.size()));
        }
        //order by regex length
        Set<IncomingPhoneNumber> regexSet = new TreeSet<IncomingPhoneNumber>(new NumberLengthComparator());
        regexSet.addAll(regexList);
        if (regexList != null && regexList.size() > 0) {
            IncomingPhoneNumber matchingRegex = findFirstMatchingRegex(numberQueries, regexSet, sourceOrganizationSid, destOrg);
            if (matchingRegex != null) {
                numberFound = matchingRegex;
            }
        }

        return numberFound;
    }

    /**
     *
     * @param inboundPhoneNumber the number to be matched agaisnt the regex list
     * @param listPhones the list of regexes to match
     * @return the first matching regex from the list
     */
    public static IncomingPhoneNumber findFirstMatchingRegex(List<String> numberQueries, Set<IncomingPhoneNumber> regexSet,
            Sid sourceOrganizationSid, Sid destOrg) {

        IncomingPhoneNumber matchedRegex = null;
        try {
            Iterator<IncomingPhoneNumber> iterator = regexSet.iterator();
            while (matchedRegex == null && iterator.hasNext()) {
                IncomingPhoneNumber currentRegex = iterator.next();
                String phoneRegexPattern = null;
                if (currentRegex.getPhoneNumber().startsWith("+")) {
                    phoneRegexPattern = currentRegex.getPhoneNumber().replace("+", "/+");
                } else if (currentRegex.getPhoneNumber().startsWith("*")) {
                    phoneRegexPattern = currentRegex.getPhoneNumber().replace("*", "/*");
                } else {
                    phoneRegexPattern = currentRegex.getPhoneNumber();
                }
                Pattern p = Pattern.compile(phoneRegexPattern);
                int i = 0;
                while (matchedRegex == null && i < numberQueries.size()) {
                    Matcher m = p.matcher(numberQueries.get(i));
                    if (m.find()) {
                        matchedRegex = currentRegex;
                    } else if (logger.isInfoEnabled()) {
                        String msg = String.format("Regex \"%s\" cannot be matched for phone number \"%s\"", phoneRegexPattern, numberQueries.get(i));
                        logger.info(msg);
                    }
                    i = i + 1;
                }
            }
            logger.info("No matching phone number found, make sure your Restcomm Regex phone number is correctly defined");
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                String msg = String.format("Exception while trying to match for a REGEX, exception: %s", e);
                logger.debug(msg);
            }
        }
        return matchedRegex;

    }
}
