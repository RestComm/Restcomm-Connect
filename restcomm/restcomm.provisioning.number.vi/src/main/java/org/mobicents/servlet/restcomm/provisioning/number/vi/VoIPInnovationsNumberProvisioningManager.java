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
package org.mobicents.servlet.restcomm.provisioning.number.vi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.provisioning.number.api.ContainerConfiguration;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberSearchFilters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;
import org.mobicents.servlet.restcomm.provisioning.number.api.ProvisionProvider;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.GetDIDListResponseConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.LATAConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.NPAConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.NXXConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.RateCenterConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.StateConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.TNConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.VoipInnovationsBodyConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.VoipInnovationsHeaderConverter;
import org.mobicents.servlet.restcomm.provisioning.number.vi.converter.VoipInnovationsResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.thoughtworks.xstream.XStream;

/**
 * @author jean
 */
public class VoIPInnovationsNumberProvisioningManager implements PhoneNumberProvisioningManager {
    private static final Logger logger = Logger.getLogger(VoIPInnovationsNumberProvisioningManager.class);
    private XStream xstream;
    private String header;
    protected Boolean telestaxProxyEnabled;
    protected String uri, username, password, endpoint;
    protected Configuration activeConfiguration;
    protected ContainerConfiguration containerConfiguration;

    public VoIPInnovationsNumberProvisioningManager() {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#init(org.apache.commons.configuration
     * .Configuration, boolean)
     */
    @Override
    public void init(Configuration phoneNumberProvisioningConfiguration, Configuration telestaxProxyConfiguration, ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            uri = telestaxProxyConfiguration.getString("uri");
            username = telestaxProxyConfiguration.getString("login");
            password = telestaxProxyConfiguration.getString("password");
            endpoint = telestaxProxyConfiguration.getString("endpoint");
            activeConfiguration = telestaxProxyConfiguration;
        } else {
            Configuration viConf = phoneNumberProvisioningConfiguration.subset("voip-innovations");
            uri = viConf.getString("uri");
            username = viConf.getString("login");
            password = viConf.getString("password");
            endpoint = viConf.getString("endpoint");
            activeConfiguration = viConf;
        }

        this.header = header(username, password);
        xstream = new XStream();
        xstream.alias("response", VoipInnovationsResponse.class);
        xstream.alias("header", VoipInnovationsHeader.class);
        xstream.alias("body", VoipInnovationsBody.class);
        xstream.alias("lata", LATAConverter.class);
        xstream.alias("npa", NPAConverter.class);
        xstream.alias("nxx", NXXConverter.class);
        xstream.alias("rate_center", RateCenterConverter.class);
        xstream.alias("state", StateConverter.class);
        xstream.alias("tn", TNConverter.class);
        xstream.registerConverter(new VoipInnovationsResponseConverter());
        xstream.registerConverter(new VoipInnovationsHeaderConverter());
        xstream.registerConverter(new VoipInnovationsBodyConverter());
        xstream.registerConverter(new GetDIDListResponseConverter());
        xstream.registerConverter(new LATAConverter());
        xstream.registerConverter(new NPAConverter());
        xstream.registerConverter(new NXXConverter());
        xstream.registerConverter(new RateCenterConverter());
        xstream.registerConverter(new StateConverter());
        xstream.registerConverter(new TNConverter());
    }

    private String header(final String login, final String password) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<header><sender>");
        buffer.append("<login>").append(login).append("</login>");
        buffer.append("<password>").append(password).append("</password>");
        buffer.append("</sender></header>");
        return buffer.toString();
    }

    private String getFriendlyName(final String number) {
        try {
            final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            final com.google.i18n.phonenumbers.Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(number, "US");
            String friendlyName = phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.E164);
            return friendlyName;
        } catch (final Exception ignored) {
            return number;
        }
    }

    private List<PhoneNumber> toAvailablePhoneNumbers(final GetDIDListResponse response, PhoneNumberSearchFilters listFilters) {
        Pattern searchPattern = listFilters.getFilterPattern();
        final List<PhoneNumber> numbers = new ArrayList<PhoneNumber>();
        final List<State> states = response.states();
        for (final State state : states) {
            if(listFilters.getInRegion() == null || (listFilters.getInRegion() != null && !listFilters.getInRegion().isEmpty() && listFilters.getInRegion().equals(state.name()))) {
                for (final LATA lata : state.latas()) {
                    for (final RateCenter center : lata.centers()) {
                        for (final NPA npa : center.npas()) {
                            for (final NXX nxx : npa.nxxs()) {
                                for (final TN tn : nxx.tns()) {
                                    final String name = getFriendlyName(tn.number());
                                    final String phoneNumber = name;
                                    if(searchPattern == null || (searchPattern != null && searchPattern.matcher(tn.number()).matches())) {
                                        if(listFilters.getFaxEnabled() == null || (listFilters.getFaxEnabled() != null && listFilters.getFaxEnabled() == tn.t38())) {
                                            // XXX Cannot know whether DID is SMS capable. Need to update to VI API 3.0 - hrosa
                                            final PhoneNumber number = new PhoneNumber(name, phoneNumber, Integer.parseInt(lata.name()),
                                                    center.name(), null, null, state.name(), null, "US", true, null, null, tn.t38(), null);
                                            numbers.add(number);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return numbers;
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
        if(logger.isDebugEnabled()) {
            logger.debug("searchPattern " + listFilters.getFilterPattern());
        }
        String areaCode = listFilters.getAreaCode();
        Pattern filterPattern = listFilters.getFilterPattern();
        String searchPattern = null;
        if(filterPattern != null) {
            searchPattern = filterPattern.toString();
        }
        if ((areaCode == null || !areaCode.isEmpty() || areaCode.length() < 3) &&
                (searchPattern != null && !searchPattern.toString().isEmpty() && searchPattern.toString().length() >= 5)) {
            areaCode = searchPattern.toString().substring(2, 5);
            if(logger.isDebugEnabled()) {
                logger.debug("areaCode derived from searchPattern " + searchPattern);
            }
        }
        if (areaCode != null && !areaCode.isEmpty() && (areaCode.length() == 3)) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<request id=\""+generateId()+"\">");
            buffer.append(header);
            buffer.append("<body>");
            buffer.append("<requesttype>").append("getDIDs").append("</requesttype>");
            buffer.append("<item>");
            buffer.append("<npa>").append(areaCode).append("</npa>");
            buffer.append("</item>");
            buffer.append("</body>");
            buffer.append("</request>");
            final String body = buffer.toString();
            final HttpPost post = new HttpPost(uri);

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("apidata", body));
            try {
                post.setEntity(new UrlEncodedFormEntity(parameters));

                final DefaultHttpClient client = new DefaultHttpClient();
                if (telestaxProxyEnabled) {
                    addTelestaxProxyHeaders(post, ProvisionProvider.REQUEST_TYPE.GETDIDS.name());
                }
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    final VoipInnovationsResponse result = (VoipInnovationsResponse) xstream.fromXML(content);
                    final GetDIDListResponse dids = (GetDIDListResponse) result.body().content();
                    if (dids.code() == 100) {
                        final List<PhoneNumber> numbers = toAvailablePhoneNumbers(dids, listFilters);
                        return numbers;
                    }
                } else {
                    logger.warn("Couldn't reach uri for getting DIDs. Response status was: "+response.getStatusLine().getStatusCode());
                }
            } catch (final Exception e) {
                logger.warn("Couldn't reach uri for getting DIDs " + uri, e);
            }
        }
        return new ArrayList<PhoneNumber>();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /*
     * (non-Javadoc)
     * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#buyNumber(java.lang.String, org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters)
     */
    @Override
    public boolean buyNumber(String phoneNumber, PhoneNumberParameters phoneNumberParameters) {
        phoneNumber = phoneNumber.substring(2);
        // Provision the number from VoIP Innovations if they own it.
        if (isValidDid(phoneNumber)) {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                final StringBuilder buffer = new StringBuilder();
                buffer.append("<request id=\""+generateId()+"\">");
                buffer.append(header);
                buffer.append("<body>");
                buffer.append("<requesttype>").append("assignDID").append("</requesttype>");
                buffer.append("<item>");
                buffer.append("<did>").append(phoneNumber).append("</did>");
                buffer.append("<endpointgroup>").append(endpoint).append("</endpointgroup>");
                buffer.append("</item>");
                buffer.append("</body>");
                buffer.append("</request>");
                final String body = buffer.toString();
                final HttpPost post = new HttpPost(uri);
                try {
                    List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                    parameters.add(new BasicNameValuePair("apidata", body));
                    post.setEntity(new UrlEncodedFormEntity(parameters));
                    final DefaultHttpClient client = new DefaultHttpClient();
                    if(telestaxProxyEnabled) {
                        addTelestaxProxyHeaders(post, ProvisionProvider.REQUEST_TYPE.ASSIGNDID.name());
                    }
                    final HttpResponse response = client.execute(post);
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        final String content = StringUtils.toString(response.getEntity().getContent());
                        if (content.contains("<statuscode>100</statuscode>")) {
                            return true;
                        }
                    }
                } catch (final Exception ignored) {
                }
            }
        }

        return false;
    }

    protected boolean isValidDid(final String did) {
        if (did != null && !did.isEmpty()) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<request id=\""+generateId()+"\">");
            buffer.append(header);
            buffer.append("<body>");
            buffer.append("<requesttype>").append("queryDID").append("</requesttype>");
            buffer.append("<item>");
            buffer.append("<did>").append(did).append("</did>");
            buffer.append("</item>");
            buffer.append("</body>");
            buffer.append("</request>");
            final String body = buffer.toString();
            final HttpPost post = new HttpPost(uri);
            try {
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                parameters.add(new BasicNameValuePair("apidata", body));
                post.setEntity(new UrlEncodedFormEntity(parameters));
                final DefaultHttpClient client = new DefaultHttpClient();
                if(telestaxProxyEnabled) {
                    addTelestaxProxyHeaders(post, ProvisionProvider.REQUEST_TYPE.QUERYDID.name());
                }
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    if (content.contains("<statusCode>100</statusCode>")) {
                        return true;
                    }
                }
            } catch (final Exception ignored) {
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#updateNumber(java.lang.String, org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters)
     */
    @Override
    public boolean updateNumber(String number, PhoneNumberParameters phoneNumberParameters) {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#cancelNumber(java.lang.String)
     */
    @Override
    public boolean cancelNumber(String phoneNumber) {
        String numberToRemoveFromVi = phoneNumber;
        if(numberToRemoveFromVi.startsWith("+1")){
            numberToRemoveFromVi = numberToRemoveFromVi.replaceFirst("\\+1", "");
        }
        phoneNumber = numberToRemoveFromVi;

        if (!isValidDid(phoneNumber))
            return false;

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<request id=\""+generateId()+"\">");
            buffer.append(header);
            buffer.append("<body>");
            buffer.append("<requesttype>").append("releaseDID").append("</requesttype>");
            buffer.append("<item>");
            buffer.append("<did>").append(phoneNumber).append("</did>");
            buffer.append("</item>");
            buffer.append("</body>");
            buffer.append("</request>");
            final String body = buffer.toString();
            final HttpPost post = new HttpPost(uri);
            try {
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                parameters.add(new BasicNameValuePair("apidata", body));
                post.setEntity(new UrlEncodedFormEntity(parameters));
                final DefaultHttpClient client = new DefaultHttpClient();
                if(telestaxProxyEnabled) {
                    addTelestaxProxyHeaders(post, ProvisionProvider.REQUEST_TYPE.RELEASEDID.name());
                }
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    if (content.contains("<statuscode>100</statuscode>")) {
                        return true;
                    }
                }
            } catch (final Exception ignored) {
            }
        }
        return false;
    }

    @Override
    public List<String> getAvailableCountries() {
        List<String> countries = new ArrayList<String>();
        countries.add("US");
        return countries;
    }

    private void addTelestaxProxyHeaders(HttpPost post, String requestType) {
        //This will work as a flag for LB that this request will need to be modified and proxied to VI
        post.addHeader("TelestaxProxy", "true");
        //Adds the Provision provider class name
        post.addHeader("Provider", ProvisionProvider.PROVIDER.VOIPINNOVATIONS.name());
        //This will tell LB that this request is a getAvailablePhoneNumberByAreaCode request
        post.addHeader("RequestType", requestType);
        //This will let LB match the DID to a node based on the node host+port
        List<SipURI> uris = containerConfiguration.getOutboundInterfaces();
        for (SipURI uri: uris) {
            post.addHeader("OutboundIntf", uri.getHost()+":"+uri.getPort()+":"+uri.getTransportParam());
        }
    }
}
