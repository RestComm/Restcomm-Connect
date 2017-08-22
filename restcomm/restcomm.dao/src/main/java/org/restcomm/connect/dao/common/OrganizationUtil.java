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
package org.restcomm.connect.dao.common;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.MostOptimalNumberResponse;
import org.restcomm.connect.dao.entities.Organization;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class OrganizationUtil {

    private static Logger logger = Logger.getLogger(OrganizationUtil.class);

    /**
     * @param storage DaoManager
     * @param request SipServletRequest
     * @param phone
     * @param sourceOrganizationSid organization Sid of request initiator
     * @return
     */
    public static MostOptimalNumberResponse getMostOptimalIncomingPhoneNumber(DaoManager storage, SipServletRequest request, String phone,
            Sid sourceOrganizationSid) {
        if(logger.isDebugEnabled())
            logger.debug("getMostOptimalIncomingPhoneNumber: "+phone);

        IncomingPhoneNumber number = null;
        boolean failCall = false;
        List<IncomingPhoneNumber> numbers = new ArrayList<IncomingPhoneNumber>();
        final IncomingPhoneNumbersDao numbersDao = storage.getIncomingPhoneNumbersDao();
        try{
            Sid destinationOrganizationSid = getOrganizationSidBySipURIHost(storage, (SipURI)request.getRequestURI());
            // try to get destinationOrganizationSid from toUril
            destinationOrganizationSid = destinationOrganizationSid != null ? destinationOrganizationSid : getOrganizationSidBySipURIHost(storage, (SipURI)request.getTo().getURI());
            if(destinationOrganizationSid == null){
                logger.error("destinationOrganizationSid is NULL: request Uri is: "+(SipURI)request.getRequestURI()+ " To Uri is: "+(SipURI)request.getTo().getURI());
            }else{
                if(logger.isDebugEnabled())
                    logger.debug("getMostOptimalIncomingPhoneNumber: sourceOrganizationSid: "+sourceOrganizationSid+" : destinationOrganizationSid: "+destinationOrganizationSid +" request Uri is: "+(SipURI)request.getRequestURI()+ " To Uri is: "+(SipURI)request.getTo().getURI());

                // Format the destination to an E.164 phone number.
                final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
                String formatedPhone = null;
                if (!(phone.contains("*") || phone.contains("#"))) {
                    try {
                        formatedPhone = phoneNumberUtil.format(phoneNumberUtil.parse(phone, "US"), PhoneNumberFormat.E164);
                    } catch (NumberParseException e) {
                        //logger.error("Exception when try to format : " + e);
                    }
                }
                if (formatedPhone == null) {
                    //Don't format to E.164 if phone contains # or * as this is
                    //for a Regex or USSD short number
                    formatedPhone = phone;
                }else {
                    //get all number with same number, by both formatedPhone and unformatedPhone
                    numbers = numbersDao.getIncomingPhoneNumber(formatedPhone);
                }
                if(numbers == null || numbers.isEmpty()){
                    numbers = numbersDao.getIncomingPhoneNumber(phone);
                }else{
                    numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
                }
                if (phone.startsWith("+")) {
                    //remove the (+) and check if exists
                    phone= phone.replaceFirst("\\+","");
                    if(numbers == null || numbers.isEmpty()){
                        numbers = numbersDao.getIncomingPhoneNumber(phone);
                    }else{
                        numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
                    }
                } else {
                    //Add "+" add check if number exists
                    phone = "+".concat(phone);
                    if(numbers == null || numbers.isEmpty()){
                        numbers = numbersDao.getIncomingPhoneNumber(phone);
                    }else{
                        numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
                    }
                }
                if(numbers == null || numbers.isEmpty()){
                    // https://github.com/Mobicents/RestComm/issues/84 using wildcard as default application
                    numbers = numbersDao.getIncomingPhoneNumber("*");
                }
                if(numbers != null && !numbers.isEmpty()){
                    // find number in same organization
                    for(IncomingPhoneNumber n : numbers){
                        if(logger.isDebugEnabled())
                            logger.debug(String.format("getMostOptimalIncomingPhoneNumber: Got a similar number from DB: Analysis report: Number:Sid = %s:%s | Is Number pure sip? %s | Number's organizations: %s", n.getPhoneNumber(), n.getSid(), n.isPureSip(), n.getOrganizationSid()));
                        if(n.getOrganizationSid().equals(destinationOrganizationSid)){
                            /*
                             * check if request is coming from same org
                             * if not then only allow provider numbers
                             */
                            if((sourceOrganizationSid != null && sourceOrganizationSid.equals(destinationOrganizationSid)) || (sourceOrganizationSid == null) || !n.isPureSip()){
                                number = n;
                                if(logger.isInfoEnabled())
                                    logger.info(String.format("Found most optimal phone number: Number:Sid = %s:%s", n.getPhoneNumber(), n.getSid()));
                            }else{
                                if(logger.isDebugEnabled())
                                    logger.debug("not allowed to call this number due to organizational restrictions");
                            }
                        }else{
                            if(logger.isDebugEnabled())
                                logger.debug(String.format("getMostOptimalIncomingPhoneNumber: Number:Sid = %s:%s does not belong to requested/destination organization: %s", n.getPhoneNumber(), n.getSid(), destinationOrganizationSid));
                        }
                        if(number != null)
                            break;
                    }
                    failCall = number == null;
                }
            }
        }catch(Exception e){
            logger.error("Error while trying to retrive getMostOptimalIncomingPhoneNumber: ", e);
        }
        if(logger.isDebugEnabled())
            logger.debug(String.format("getMostOptimalIncomingPhoneNumber Resut: Found the number? %s | Is this number relevant? %s", number!=null, failCall));
        return new MostOptimalNumberResponse(number, failCall);
    }


    /**
     * getOrganizationSidBySipURIHost
     *
     * @param sipURI
     * @return Sid of Organization
     */
    public static Sid getOrganizationSidBySipURIHost(DaoManager storage, final SipURI sipURI){
        if(logger.isDebugEnabled())
            logger.debug(String.format("getOrganizationSidBySipURIHost sipURI = %s", sipURI));
        final String organizationDomainName = sipURI.getHost();
        Organization organization = storage.getOrganizationsDao().getOrganizationByDomainName(organizationDomainName);
        return organization == null ? null : organization.getSid();
    }

    /**
     * getOrganizationSidByAccountSid
     * @param accountSid
     * @return Sid of Organization
     */
    public static Sid getOrganizationSidByAccountSid(DaoManager storage, final Sid accountSid){
        return storage.getAccountsDao().getAccount(accountSid).getOrganizationSid();
    }
}
