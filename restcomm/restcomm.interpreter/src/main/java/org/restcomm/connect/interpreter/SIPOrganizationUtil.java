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

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Organization;
import org.restcomm.connect.dao.OrganizationsDao;

public class SIPOrganizationUtil {

    private static Logger logger = Logger.getLogger(SIPOrganizationUtil.class);

    public static Sid getOrganizationSidBySipURIHost(OrganizationsDao orgDao, final SipURI sipURI) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("getOrganizationSidBySipURIHost sipURI = %s", sipURI));
        }
        final String organizationDomainName = sipURI.getHost();
        Organization organization = orgDao.getOrganizationByDomainName(organizationDomainName);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Org found = %s", organization));
        }
        return organization == null ? null : organization.getSid();
    }

    public static Sid searchOrganizationBySIPRequest(OrganizationsDao orgDao, SipServletRequest request)
    {
        Sid destinationOrganizationSid = null;
        SipURI sipURI = null;
        if (!request.getMethod().equals("REFER")) {
            //first try with requetURI
            destinationOrganizationSid = getOrganizationSidBySipURIHost(orgDao, (SipURI)request.getRequestURI());
            // try to get destinationOrganizationSid from toUri
            if (destinationOrganizationSid == null) {
                destinationOrganizationSid = getOrganizationSidBySipURIHost(orgDao, (SipURI)request.getTo().getURI());
            }
        }else{
            // The Request URI from SIP REFER method is going with the IP Address instead of the domain name
            // try to get destinationOrganizationSid from Refer-To
            try{
                sipURI = (SipURI)request.getAddressHeader("Refer-To").getURI();
            } catch (ServletParseException e){
                logger.error("sipURI is NULL");
            }
            if (sipURI != null){
                destinationOrganizationSid = getOrganizationSidBySipURIHost(orgDao, sipURI);
                if(destinationOrganizationSid == null){
                    logger.error("destinationOrganizationSid is NULL: Refer-To Uri is: "+ sipURI);
                }else{
                    logger.debug("searchOrganizationBySIPRequest: destinationOrganizationSid: "+destinationOrganizationSid +" Refer-To Uri is: "+sipURI);
                }
            }
        }
        return destinationOrganizationSid;
    }
}
