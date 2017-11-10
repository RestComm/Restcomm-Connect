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
package org.restcomm.connect.http.client.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.http.asyncclient.HttpAsycClientHelper;
import org.restcomm.connect.http.client.DownloaderResponse;
import org.restcomm.connect.http.client.HttpRequestDescriptor;
import org.restcomm.connect.telephony.api.Hangup;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author mariafarooq
 * Disposable call api client to be used for one request and get destroyed after that.
 *
 */
public class CallApiClient extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final DaoManager storage;
    private final Sid callSid;
    private CallDetailRecord callDetailRecord;
    private static final String SUPER_ADMIN_ACCOUNT_SID="ACae6e420f425248d6a26948c17a9e2acf";
    private ActorRef requestee;

    private ActorRef httpAsycClientHelper;

    public CallApiClient(final Sid callSid, final DaoManager storage) {
        super();
        this.callSid = callSid;
        this.storage = storage;
    }
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** CallApiClient " + self().path() + " Sender: " + sender);
            logger.info(" ********** CallApiClient " + self().path() + " Processing Message: " + klass.getName());
        }
        if (Hangup.class.equals(klass)) {
            onHangup((Hangup) message, self, sender);
        } else if (DownloaderResponse.class.equals(klass)) {
            onDownloaderResponse(message, self, sender);
            // since this is a one time disposable client lets clean it off after it sends DownloaderResponse.
            getContext().stop(self());
        }
    }

    private void onDownloaderResponse(Object message, ActorRef self, ActorRef sender) {
        final DownloaderResponse response = (DownloaderResponse) message;
        if (logger.isInfoEnabled()) {
            logger.info("Call api response succeeded " + response.succeeded());
            logger.info("Call api response: " + response);
        }
        requestee.tell(message, self);
    }

    protected void onHangup(Hangup message, ActorRef self, ActorRef sender) throws URISyntaxException, ParseException {
        requestee = sender;
        callDetailRecord = getCallDetailRecord();
        if(callDetailRecord == null){
            logger.error("could not retrieve conferenceDetailRecord");
        } else {
            try {
                URI uri = new URI("/restcomm"+callDetailRecord.getUri());
                uri = UriUtils.resolve(uri);
                if (logger.isInfoEnabled())
                    logger.info("call api uri is: "+uri);

                Header[] headers = {
                        new BasicHeader(HttpHeaders.AUTHORIZATION, getSuperAdminAuthenticationHeader())
                        ,new BasicHeader("Content-type", "application/x-www-form-urlencoded")
                        ,new BasicHeader("Accept", "application/json")
                    };

                ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
                postParameters.add(new BasicNameValuePair("Status", "Completed"));

                httpAsycClientHelper = httpAsycClientHelper();
                HttpRequestDescriptor httpRequestDescriptor =new HttpRequestDescriptor(uri, "POST", postParameters, -1, headers);
                httpAsycClientHelper.tell(httpRequestDescriptor, self);
            } catch (Exception e) {
                logger.error("Exception while trying to terminate call via api: ", e);
            }
        }
    }

    private String getSuperAdminAuthenticationHeader(){
        Account superAdminAccount = storage.getAccountsDao().getAccount(SUPER_ADMIN_ACCOUNT_SID);

        String auth = superAdminAccount.getSid() + ":" + superAdminAccount.getAuthToken();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
        return "Basic " + new String(encodedAuth);
    }

    private CallDetailRecord getCallDetailRecord() {
        if(callSid != null){
            return storage.getCallDetailRecordsDao().getCallDetailRecord(callSid);
        }
        return null;
    }

    private ActorRef httpAsycClientHelper(){
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new HttpAsycClientHelper();
            }
        });
        return getContext().actorOf(props);

    }

    @Override
    public void postStop () {
        if (logger.isInfoEnabled()) {
            logger.info("CallApiClient at post stop");
        }
        if(httpAsycClientHelper != null && !httpAsycClientHelper.isTerminated())
            getContext().stop(httpAsycClientHelper);
        getContext().stop(self());
        super.postStop();
    }
}
