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
package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumber;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumberList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.http.converter.AvailablePhoneNumberConverter;
import org.mobicents.servlet.restcomm.http.converter.AvailablePhoneNumberListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.GetDIDListResponse;
import org.mobicents.servlet.restcomm.http.voipinnovations.LATA;
import org.mobicents.servlet.restcomm.http.voipinnovations.NPA;
import org.mobicents.servlet.restcomm.http.voipinnovations.NXX;
import org.mobicents.servlet.restcomm.http.voipinnovations.RateCenter;
import org.mobicents.servlet.restcomm.http.voipinnovations.State;
import org.mobicents.servlet.restcomm.http.voipinnovations.TN;
import org.mobicents.servlet.restcomm.http.voipinnovations.VoipInnovationsBody;
import org.mobicents.servlet.restcomm.http.voipinnovations.VoipInnovationsHeader;
import org.mobicents.servlet.restcomm.http.voipinnovations.VoipInnovationsResponse;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.GetDIDListResponseConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.LATAConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.NPAConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.NXXConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.RateCenterConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.StateConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.TNConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.VoipInnovationsBodyConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.VoipInnovationsHeaderConverter;
import org.mobicents.servlet.restcomm.http.voipinnovations.converter.VoipInnovationsResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public abstract class AvailablePhoneNumbersEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected Configuration voipInnovationsConfiguration;
    protected Configuration telestaxProxyConfiguration;
    protected Configuration activeConfiguration;
    protected Boolean telestaxProxyEnabled;
    protected String uri, username, password, endpoint;
    private XStream xstream;
    protected Gson gson;

    private String header;

    public AvailablePhoneNumbersEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        super.init(configuration.subset("runtime-settings"));
        voipInnovationsConfiguration = configuration.subset("voip-innovations");
        telestaxProxyConfiguration = configuration.subset("runtime-settings").subset("telestax-proxy");
        telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            uri = telestaxProxyConfiguration.getString("uri");
            username = telestaxProxyConfiguration.getString("login");
            password = telestaxProxyConfiguration.getString("password");
            endpoint = telestaxProxyConfiguration.getString("endpoint");
            activeConfiguration = telestaxProxyConfiguration;
        } else {
            uri = voipInnovationsConfiguration.getString("uri");
            username = voipInnovationsConfiguration.getString("login");
            password = voipInnovationsConfiguration.getString("password");
            endpoint = voipInnovationsConfiguration.getString("endpoint");
            activeConfiguration = voipInnovationsConfiguration;
        }
        this.header = header(username, password);
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(new AvailablePhoneNumberConverter(activeConfiguration));
        xstream.registerConverter(new AvailablePhoneNumberListConverter(activeConfiguration));
        xstream.registerConverter(new RestCommResponseConverter(activeConfiguration));
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
        final GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();

    }

    private String header(final String login, final String password) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<header><sender>");
        buffer.append("<login>").append(login).append("</login>");
        buffer.append("<password>").append(password).append("</password>");
        buffer.append("</sender></header>");
        return buffer.toString();
    }

    protected Response getAvailablePhoneNumbersByAreaCode(final String accountSid, final String areaCode,
            final MediaType responseType) {
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
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    final VoipInnovationsResponse result = (VoipInnovationsResponse) xstream.fromXML(content);
                    final GetDIDListResponse dids = (GetDIDListResponse) result.body().content();
                    if (dids.code() == 100) {
                        final List<AvailablePhoneNumber> numbers = toAvailablePhoneNumbers(dids);
                        if (APPLICATION_XML_TYPE == responseType) {
                            return ok(xstream.toXML(new RestCommResponse(new AvailablePhoneNumberList(numbers))),
                                    APPLICATION_XML).build();
                        } else if (APPLICATION_JSON_TYPE == responseType) {
                            return ok(gson.toJson(numbers), APPLICATION_JSON).build();
                        }
                    }
                }
            } catch (final Exception ignored) {
            }
            return status(INTERNAL_SERVER_ERROR).build();
        } else {
            return status(BAD_REQUEST).build();
        }
    }

    private String getFriendlyName(final String number) {
        try {
            final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            final PhoneNumber phoneNumber = phoneNumberUtil.parse(number, "US");
            String friendlyName = phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.E164);
            return friendlyName;
        } catch (final Exception ignored) {
            return number;
        }
    }

    private List<AvailablePhoneNumber> toAvailablePhoneNumbers(final GetDIDListResponse response) {
        final List<AvailablePhoneNumber> numbers = new ArrayList<AvailablePhoneNumber>();
        final State state = response.state();
        for (final LATA lata : state.latas()) {
            for (final RateCenter center : lata.centers()) {
                for (final NPA npa : center.npas()) {
                    for (final NXX nxx : npa.nxxs()) {
                        for (final TN tn : nxx.tns()) {
                            final String name = getFriendlyName(tn.number());
                            final String phoneNumber = name;
                            // XXX Cannot know whether DID is SMS capable. Need to update to VI API 3.0 - hrosa
                            final AvailablePhoneNumber number = new AvailablePhoneNumber(name, phoneNumber,
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
}
