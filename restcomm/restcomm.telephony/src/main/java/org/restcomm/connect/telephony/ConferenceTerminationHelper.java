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
package org.restcomm.connect.telephony;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;
import org.restcomm.connect.http.client.Downloader;
import org.restcomm.connect.http.client.HttpRequestDescriptor;
import org.restcomm.connect.telephony.api.StopConference;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ConferenceTerminationHelper extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final DaoManager storage;
    private final String accountSid;
    private final String name;
    private final Sid conferenceSid;
    private ConferenceDetailRecord conferenceDetailRecord;
	private URI conferenceURI;
	private static final String SUPER_ADMIN_ACCOUNT_SID="ACae6e420f425248d6a26948c17a9e2acf";

    public ConferenceTerminationHelper(final String accountSid, final String name, final Sid conferenceSid, final DaoManager storage) {
        super();
        this.accountSid = accountSid;
        this.name = name;
        this.conferenceSid = conferenceSid;
        this.storage = storage;
    }
    @Override
	public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** ConferenceTerminationHelper " + self().path() + " Sender: " + sender);
            logger.info(" ********** ConferenceTerminationHelper " + self().path() + " Processing Message: " + klass.getName());
        }
        if (StopConference.class.equals(klass)) {
            onStopConference((StopConference) message, self, sender);
        }
	}

    protected void onStopConference(StopConference message, ActorRef self, ActorRef sender) throws URISyntaxException, ParseException {
        conferenceDetailRecord = getConferenceDetailRecord();
        if(conferenceDetailRecord == null){
        	logger.error("could not retrieve conferenceDetailRecord");
        } else {
        	Account superAdminAccount = storage.getAccountsDao().getAccount(SUPER_ADMIN_ACCOUNT_SID);
    		ActorRef downloader = downloader();
    		URI uri = new URI(String.format("/restcomm/%s/Accounts/%s/Conferences/%s", RestcommConfiguration.getInstance().getMain().getApiVersion(), SUPER_ADMIN_ACCOUNT_SID, conferenceDetailRecord.getSid())); 

            final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("Status", "Completed"));
    		downloader.tell(new HttpRequestDescriptor(uri, "POST", parameters), self);
        }
	}

	protected ActorRef downloader() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Downloader();
            }
        });
        return getContext().actorOf(props);
    }

	protected ConferenceDetailRecord getConferenceDetailRecord() throws ParseException{
		if(conferenceSid == null){
			ConferenceDetailRecordFilter filter = new ConferenceDetailRecordFilter(accountSid, "RUNNING%", null, null, name, 1, 0);
			List<ConferenceDetailRecord> conferenceDetailRecords = storage.getConferenceDetailRecordsDao().getConferenceDetailRecords(filter);
			if(conferenceDetailRecords == null || conferenceDetailRecords.isEmpty()){
				logger.warning(String.format("could not retrive any conference record for this conference accountSid %s name: %s", accountSid, name));
				return null;
			}else{
				return conferenceDetailRecords.get(0);
			}
		} else {
			return storage.getConferenceDetailRecordsDao().getConferenceDetailRecord(conferenceSid);
		}
	}
}
