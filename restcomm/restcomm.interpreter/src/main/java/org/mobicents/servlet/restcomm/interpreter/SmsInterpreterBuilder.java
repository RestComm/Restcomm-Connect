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

import java.net.URI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Sid;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsInterpreterBuilder {
  private final ActorSystem system;
  private Configuration configuration;
  private ActorRef service;
  private DaoManager storage;
  private Sid accountId;
  private String version;
  private URI url;
  private String method;
  private URI fallbackUrl;
  private String fallbackMethod;

  public SmsInterpreterBuilder(final ActorSystem system) {
    super();
    this.system = system;
  }
  
  public ActorRef build() {
    return system.actorOf(new Props(new UntypedActorFactory() {
	  private static final long serialVersionUID = 1L;
	  @Override public UntypedActor create() throws Exception {
		return new SmsInterpreter(service, configuration, storage, accountId, version,
		    url, method, fallbackUrl, fallbackMethod);
	  }
    }));
  }
  
  public void setConfiguration(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  public void setSmsService(final ActorRef service) {
    this.service = service;
  }
  
  public void setStorage(final DaoManager storage) {
    this.storage = storage;
  }
  
  public void setAccount(final Sid accountId) {
    this.accountId = accountId;
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
  
  public void setVersion(final String version) {
    this.version = version;
  }
}
