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
package org.mobicents.servlet.restcomm.interpreter;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import java.net.URI;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SubVoiceInterpreterBuilder {
  private final ActorSystem system;
  private Configuration configuration;
  private DaoManager storage;
  private ActorRef calls;
  private ActorRef conferences;
  private ActorRef sms;
  private Sid account;
  private Sid phone;
  private String version;
  private URI url;
  private String method;
  private URI fallbackUrl;
  private String fallbackMethod;
  private URI statusCallback;
  private String statusCallbackMethod;
  private String emailAddress;
  
  private Boolean hangupOnEnd = false;

  /**
   * @author thomas.quintana@telestax.com (Thomas Quintana)
   */
  public SubVoiceInterpreterBuilder(final ActorSystem system) {
    super();
    this.system = system;
  }
  
  public ActorRef build() {
    return system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
          return new SubVoiceInterpreter(configuration, account, phone, version, url, method,
              fallbackUrl, fallbackMethod, statusCallback, statusCallbackMethod, emailAddress,
              calls, conferences, sms, storage, hangupOnEnd);
		}
    }));
  }
  
  public void setConfiguration(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  public void setStorage(final DaoManager storage) {
    this.storage = storage;
  }
  
  public void setCallManager(final ActorRef calls) {
    this.calls = calls;
  }
  
  public void setConferenceManager(final ActorRef conferences) {
    this.conferences = conferences;
  }
  
  public void setSmsService(final ActorRef sms) {
    this.sms = sms;
  }
  
  public void setAccount(final Sid account) {
    this.account = account;
  }
  
  public void setPhone(final Sid phone) {
    this.phone = phone;
  }
  
  public void setUrl(final URI url) {
    this.url = url;
  }
  
  public void setMethod(final String method) {
    this.method = method;
  }
  
  public void setFallbackUrl(final URI fallbackUrl) {
    this.fallbackUrl = fallbackUrl;
  }
  
  public void setFallbackMethod(final String fallbackMethod) {
    this.fallbackMethod = fallbackMethod;
  }
  
  public void setStatusCallback(final URI statusCallback) {
    this.statusCallback = statusCallback;
  }
  
  public void setStatusCallbackMethod(final String statusCallbackMethod) {
    this.statusCallbackMethod = statusCallbackMethod;
  }
  
  public void setEmailAddress(final String emailAddress) {
    this.emailAddress = emailAddress;
  }
  
  public void setVersion(final String version) {
    this.version = version;
  }
  
  public void setHangupOnEnd(final Boolean hangupOnEnd) {
	  this.hangupOnEnd = hangupOnEnd;
  }
}