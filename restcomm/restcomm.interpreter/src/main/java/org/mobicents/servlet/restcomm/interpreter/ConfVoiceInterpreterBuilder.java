package org.mobicents.servlet.restcomm.interpreter;

import java.net.URI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.telephony.CallInfo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

public class ConfVoiceInterpreterBuilder {

	private ActorSystem system;
	private Configuration configuration;
	private Sid account;
	private String version;
	private URI url;
	private String method;
	private String emailAddress;
	private ActorRef conference;
	private DaoManager storage;
	private CallInfo callInfo;

	public ConfVoiceInterpreterBuilder(final ActorSystem system) {
		super();
		this.system = system;
	}

	public ActorRef build() {
		return system.actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;

			@Override
			public UntypedActor create() throws Exception {
				return new ConfVoiceInterpreter(configuration, account, version, url, method, emailAddress, conference,
						storage, callInfo);
			}
		}));
	}

	public void setConfiguration(final Configuration configuration) {
		this.configuration = configuration;
	}

	public void setAccount(final Sid account) {
		this.account = account;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public void setUrl(final URI url) {
		this.url = url;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public void setEmailAddress(final String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setConference(final ActorRef conference) {
		this.conference = conference;
	}

	public void setStorage(final DaoManager storage) {
		this.storage = storage;
	}
	
	public void setCallInfo(CallInfo callInfo){
		this.callInfo = callInfo;
	}

}
