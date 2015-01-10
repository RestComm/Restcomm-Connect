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

package org.mobicents.servlet.restcomm.provisioning.number.bandwidth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.sip.SipURI;
import javax.xml.stream.XMLInputFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.provisioning.number.api.ContainerConfiguration;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberSearchFilters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberType;
import org.mobicents.servlet.restcomm.provisioning.number.api.ProvisionProvider;
import org.mobicents.servlet.restcomm.provisioning.number.bandwidth.utils.XmlUtils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

/**
 * @author sbarstow@bandwidth.com
 */
public class BandwidthNumberProvisioningManager implements PhoneNumberProvisioningManager {

    private static final Logger logger = Logger.getLogger(BandwidthNumberProvisioningManager.class);

    protected String uri, username, password, accountId, siteId;
    protected Configuration activeConfiguration;
    protected ContainerConfiguration containerConfiguration;
    protected boolean telestaxProxyEnabled;
    private DefaultHttpClient httpClient;
    private XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    public BandwidthNumberProvisioningManager() {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#init(org.apache.commons.configuration
     * .Configuration, boolean)
     */
    @Override
    public void init(org.apache.commons.configuration.Configuration phoneNumberProvisioningConfiguration,
            org.apache.commons.configuration.Configuration telestaxProxyConfiguration, ContainerConfiguration
            containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            uri = telestaxProxyConfiguration.getString("uri");
            username = telestaxProxyConfiguration.getString("login");
            password = telestaxProxyConfiguration.getString("password");
            accountId = telestaxProxyConfiguration.getString("endpoint");
            siteId = telestaxProxyConfiguration.getString("siteId");
            activeConfiguration = telestaxProxyConfiguration;
        } else {
            Configuration bandwidthConfiguration = phoneNumberProvisioningConfiguration.subset("bandwidth");
            uri = bandwidthConfiguration.getString("uri");
            username = bandwidthConfiguration.getString("username");
            password = bandwidthConfiguration.getString("password");
            accountId = bandwidthConfiguration.getString("accountId");
            siteId = bandwidthConfiguration.getString("siteId");
            activeConfiguration = bandwidthConfiguration;
        }
        httpClient = new DefaultHttpClient();
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
    }

    public List<String> getAvailableCountries() {
        List<String> countries = new ArrayList<String>();
        countries.add("US");
        return countries;
    }

    public boolean buyNumber(String phoneNumber, PhoneNumberParameters parameters) {
        boolean isSucceeded = false;
        phoneNumber = phoneNumber.substring(2); //we don't want the +1
        Order order = new Order();
        order.setSiteId(this.siteId);
        order.setName("Order For Number: " + phoneNumber);
        ExistingTelephoneNumberOrderType existingTelephoneNumberOrderType = new ExistingTelephoneNumberOrderType();
        existingTelephoneNumberOrderType.getTelephoneNumberList().add(phoneNumber);
        order.setExistingTelephoneNumberOrderType(existingTelephoneNumberOrderType);
        try {
            HttpPost post = new HttpPost(buildOrdersUri());
            String xml = XmlUtils.toXml(order);
            StringEntity entity = new StringEntity(xml, ContentType.APPLICATION_XML);
            post.setEntity(entity);
            if (telestaxProxyEnabled)
                addTelestaxProxyHeaders(post, ProvisionProvider.REQUEST_TYPE.ASSIGNDID.name());
            OrderResponse response = (OrderResponse) XmlUtils.fromXml(executeRequest(post), OrderResponse.class);
            if (response.getOrder().getExistingTelephoneNumberOrderType().getTelephoneNumberList().get(0).equals(phoneNumber)) {
                isSucceeded = true;
            }
        } catch (Exception e) {
            logger.error("Error creating order: " + e.getMessage());
            isSucceeded = false;
        }

        return isSucceeded;
    }

    public boolean cancelNumber(String phoneNumber) {
        boolean isSucceeded = false;
        phoneNumber = phoneNumber.substring(2);
        DisconnectTelephoneNumberOrder order = new DisconnectTelephoneNumberOrder();
        order.setName("Disconnect Order For Number: " + phoneNumber);
        DisconnectTelephoneNumberOrderType disconnectTelephoneNumberOrderType = new DisconnectTelephoneNumberOrderType();
        disconnectTelephoneNumberOrderType.getTelephoneNumberList().add(phoneNumber);
        order.setDisconnectTelephoneNumberOrderType(disconnectTelephoneNumberOrderType);
        try {
            HttpPost post = new HttpPost(buildDisconnectsUri());
            StringEntity entity = new StringEntity(XmlUtils.toXml(order), ContentType.APPLICATION_XML);
            post.setEntity(entity);
            if (telestaxProxyEnabled)
                addTelestaxProxyHeaders(post, ProvisionProvider.REQUEST_TYPE.RELEASEDID.name());
            DisconnectTelephoneNumberOrderResponse response = (DisconnectTelephoneNumberOrderResponse)
                    XmlUtils.fromXml(executeRequest(post), DisconnectTelephoneNumberOrderResponse.class);
            if (response.getErrorList().size() == 0 && response.getorderRequest().
                    getDisconnectTelephoneNumberOrderType().getTelephoneNumberList().get(0).equals(phoneNumber)) {
                isSucceeded = true;
            }
            //TODO: get order status and check it before returning.
        } catch (Exception e) {
            logger.error(String.format("Error disconnecting number: %s : %s ", phoneNumber, e.getMessage()));
            isSucceeded = false;
        }
        return isSucceeded;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#searchForNumbers(java.lang.String,
     * java.lang.String, java.util.regex.Pattern, boolean, boolean, boolean, boolean, int, int)
     */
    @Override
    public List<PhoneNumber> searchForNumbers(String country, PhoneNumberSearchFilters listFilters) {
        List<PhoneNumber> availableNumbers = new ArrayList<PhoneNumber>();
        if (logger.isDebugEnabled()) {
            logger.debug("searchPattern: " + listFilters.getFilterPattern());
        }
        try {
            String uri = buildSearchUri(listFilters);
            HttpGet httpGet = new HttpGet(uri);
            if (telestaxProxyEnabled)
                addTelestaxProxyHeaders(httpGet, ProvisionProvider.REQUEST_TYPE.GETDIDS.name());
            String response = executeRequest(httpGet);
            availableNumbers = toPhoneNumbers((SearchResult) XmlUtils.fromXml(response, SearchResult.class));
            return availableNumbers;

        } catch (Exception e) {
            logger.error("Could not execute search request: " + uri, e);
        }
        return availableNumbers;
    }

    public boolean updateNumber(String number, PhoneNumberParameters parameters) {
        return true;
    }

    private String buildDisconnectsUri() throws URISyntaxException {
        URIBuilder builder = new URIBuilder(this.uri);
        builder.setPath("/v1.0/accounts/" + this.accountId + "/disconnects");
        return builder.build().toString();
    }

    private String buildOrdersUri() throws URISyntaxException {
        URIBuilder builder = new URIBuilder(this.uri);
        builder.setPath("/v1.0/accounts/" + this.accountId + "/orders");
        return builder.build().toString();
    }

    private String buildSearchUri(PhoneNumberSearchFilters filters) throws URISyntaxException {
        Pattern filterPattern = filters.getFilterPattern();

        URIBuilder builder = new URIBuilder(this.uri);
        builder.setPath("/v1.0/accounts/" + this.accountId + "/availableNumbers");

        //Local number type search
        if (filters.getPhoneNumberTypeSearch().equals(PhoneNumberType.Local)) {
            if (!StringUtils.isEmpty(filters.getAreaCode())) {
                builder.addParameter("areaCode", filters.getAreaCode());
            }
            if (!StringUtils.isEmpty(filters.getInLata())) {
                builder.addParameter("lata", filters.getInLata());
            }
            if (!StringUtils.isEmpty(filters.getInPostalCode())) {
                builder.addParameter("zip", filters.getInPostalCode());
            }
            if (!StringUtils.isEmpty(filters.getInRateCenter())) {
                builder.addParameter("rateCenter", filters.getInRateCenter());
                builder.addParameter("state", filters.getInRegion());
            }
            builder.addParameter("enableTNDetail", String.valueOf(true));

        } else if (filters.getPhoneNumberTypeSearch().equals(PhoneNumberType.TollFree)) {
            //Make some assumptions for the user
            if (filterPattern == null || StringUtils.isEmpty(filterPattern.toString())) {
                builder.addParameter("tollFreeWildCardPattern", "8**");
            } else {
                if (filterPattern.toString().contains("*")) {
                    builder.addParameter("tollFreeWildCardPattern", filterPattern.toString());
                } else {
                    builder.addParameter("tollFreeVanity", filterPattern.toString());
                }
            }

        } else {
            logger.error("Phone Number Type: " + filters.getPhoneNumberTypeSearch().name() + " is not supported");
        }
        builder.addParameter("quantity", String.valueOf(filters.getRangeSize() == -1 ? 5 : filters.getRangeSize()));
        logger.debug("building uri: " + builder.build().toString());
        return builder.build().toString();
    }

    protected String executeRequest(HttpUriRequest request) throws IOException {
        String response = "";
        try {
            HttpResponse httpResponse = httpClient.execute(request);
            response = httpResponse.getEntity() != null ? EntityUtils.toString(httpResponse.getEntity()) : "";
        } catch (ClientProtocolException cpe) {
            logger.error("Error in execute request: " + cpe.getMessage());
            throw new IOException(cpe);
        }
        return response;
    }

    private List<PhoneNumber> toPhoneNumbers(final SearchResult searchResult) {
        final List<PhoneNumber> numbers = new ArrayList<>();
        if (searchResult.getTelephoneNumberDetailList().size() > 0) {
            for (final TelephoneNumberDetail detail : searchResult.getTelephoneNumberDetailList()) {
                String name = getFriendlyName(detail.getFullNumber(), "US");
                final PhoneNumber phoneNumber = new PhoneNumber(name,
                        name, Integer.parseInt(detail.getLATA()), detail.getRateCenter(), null, null,
                        detail.getState(), null, "US", true, true, false, false, false);
                numbers.add(phoneNumber);
            }
        } else if (searchResult.getTelephoneNumberList().size() > 0) {
            for (final String number : searchResult.getTelephoneNumberList()) {
                String name = getFriendlyName(number, "US");
                final PhoneNumber phoneNumber = new PhoneNumber(name, name, null, null, null, null,
                        null, null, "US", true, true, false, false, false);
                numbers.add(phoneNumber);
            }
        }
        return numbers;
    }

    private String getFriendlyName(final String number, String countryCode) {
        try {
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(number, countryCode);
            String friendlyName = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            return friendlyName;
        } catch (final Exception ignored) {
            return number;
        }
    }

    private void addTelestaxProxyHeaders(HttpRequest httpRequest, String requestType) {
        //This will work as a flag for LB that this request will need to be modified and proxied to VI
        httpRequest.addHeader("TelestaxProxy", "true");
        //Adds the Provision provider class name
        httpRequest.addHeader("Provider", ProvisionProvider.PROVIDER.BANDWIDTH.name());
        //This will tell LB that this request is a getAvailablePhoneNumberByAreaCode request
        httpRequest.addHeader("RequestType", requestType);
        //This is will add the instance id for the CancelNumber request that is missing SiteId from the request body
        httpRequest.addHeader("SiteId", siteId);
        //This will let LB match the DID to a node based on the node host+port
        List<SipURI> uris = containerConfiguration.getOutboundInterfaces();
        for (SipURI uri: uris) {
            httpRequest.addHeader("OutboundIntf", uri.getHost()+":"+uri.getPort()+":"+uri.getTransportParam());
        }
    }

}