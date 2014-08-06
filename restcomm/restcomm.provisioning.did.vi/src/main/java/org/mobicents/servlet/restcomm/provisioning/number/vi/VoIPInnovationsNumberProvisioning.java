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

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;
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
 *
 */
public class VoIPInnovationsNumberProvisioning implements
		PhoneNumberProvisioningManager {

	private boolean teleStaxProxyEnabled;
	private XStream xstream;
    private String header;
    protected Boolean telestaxProxyEnabled;
    protected String uri, username, password, endpoint;
    protected Configuration activeConfiguration;
    
    public VoIPInnovationsNumberProvisioning() {
	}
    
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#init(org.apache.commons.configuration.Configuration, boolean)
	 */
	@Override
	public void init(Configuration phoneNumberProvisioningConfiguration, Configuration telestaxProxyConfiguration) {
		telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            uri = telestaxProxyConfiguration.getString("uri");
            username = telestaxProxyConfiguration.getString("login");
            password = telestaxProxyConfiguration.getString("password");
            endpoint = telestaxProxyConfiguration.getString("endpoint");
            activeConfiguration = telestaxProxyConfiguration;
        } else {
            uri = phoneNumberProvisioningConfiguration.getString("uri");
            username = phoneNumberProvisioningConfiguration.getString("login");
            password = phoneNumberProvisioningConfiguration.getString("password");
            endpoint = phoneNumberProvisioningConfiguration.getString("endpoint");
            activeConfiguration = phoneNumberProvisioningConfiguration;
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

    private List<PhoneNumber> toAvailablePhoneNumbers(final GetDIDListResponse response) {
        final List<PhoneNumber> numbers = new ArrayList<PhoneNumber>();
        final State state = response.state();
        for (final LATA lata : state.latas()) {
            for (final RateCenter center : lata.centers()) {
                for (final NPA npa : center.npas()) {
                    for (final NXX nxx : npa.nxxs()) {
                        for (final TN tn : nxx.tns()) {
                            final String name = getFriendlyName(tn.number());
                            final String phoneNumber = name;
                            // XXX Cannot know whether DID is SMS capable. Need to update to VI API 3.0 - hrosa
                            final PhoneNumber number = new PhoneNumber(name, phoneNumber,
                                    Integer.parseInt(lata.name()), center.name(), null, null, state.name(), null, "US", true,
                                    null, null, tn.t38());
                            numbers.add(number);
                        }
                    }
                }
            }
        }
        return numbers;
    }
	
	/*
	 * (non-Javadoc)
	 * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#searchForNumbers(java.lang.String, java.lang.String, java.lang.String, boolean, boolean, boolean, boolean, int, int)
	 */
	@Override
	public List<PhoneNumber> searchForNumbers(String country, String areaCode, String searchPattern, boolean smsEnabled, boolean mmsEnabled, boolean voiceEnabled,
	boolean faxEnabled, int rangeSize, int rangeIndex) {
		if (areaCode != null && !areaCode.isEmpty() && (areaCode.length() == 3)) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<request id=\"\">");
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
            try {
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                parameters.add(new BasicNameValuePair("apidata", body));
                post.setEntity(new UrlEncodedFormEntity(parameters));
                final DefaultHttpClient client = new DefaultHttpClient();
                if(teleStaxProxyEnabled) {
                    //This will work as a flag for LB that this request will need to be modified and proxied to VI
                    post.addHeader("TelestaxProxy", String.valueOf(teleStaxProxyEnabled));
                    //This will tell LB that this request is a getAvailablePhoneNumberByAreaCode request
                    post.addHeader("RequestType", "GetAvailablePhoneNumbersByAreaCode");
                }
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    final VoipInnovationsResponse result = (VoipInnovationsResponse) xstream.fromXML(content);
                    final GetDIDListResponse dids = (GetDIDListResponse) result.body().content();
                    if (dids.code() == 100) {
                        final List<PhoneNumber> numbers = toAvailablePhoneNumbers(dids);
                        return numbers;
                    }
                }
            } catch (final Exception ignored) {
            }
        }
		return new ArrayList<PhoneNumber>();
	}

	/* (non-Javadoc)
	 * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#buyNumber(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean buyNumber(String country, String number, String smsHttpURL,
			String smsType, String voiceURL) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#updateNumber(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean updateNumber(String country, String number,
			String smsHttpURL, String smsType, String voiceURL) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#cancelNumber(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean cancelNumber(String country, String number) {
		// TODO Auto-generated method stub
		return false;
	}

}
