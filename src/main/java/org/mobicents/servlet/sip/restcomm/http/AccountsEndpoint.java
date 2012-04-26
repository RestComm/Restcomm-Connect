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
package org.mobicents.servlet.sip.restcomm.http;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.http.converter.AccountConverter;

import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts")
@ThreadSafe public final class AccountsEndpoint extends AbstractEndpoint {
  private final AccountsDao dao;
  private final XStream xstream;
  
  public AccountsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getAccountsDao();
    xstream = new XStream();
    xstream.alias("Account", Account.class);
    xstream.registerConverter(new AccountConverter("2012-04-24"));
  }
  
  private Account createFrom(final MultivaluedMap<String, String> data) {
    validate(data);
    final Sid sid = Sid.generate(Sid.Type.ACCOUNT);
    final DateTime now = DateTime.now();
    final String emailAddress = data.getFirst("EmailAddress");
    String friendlyName = emailAddress;
    if(data.containsKey("FriendlyName")) {
      friendlyName = data.getFirst("FriendlyName");
    }
    final Account.Type type = Account.Type.FULL;
    Account.Status status = Account.Status.ACTIVE;
    if(data.containsKey("Status")) {
      status = Account.Status.valueOf(data.getFirst("Status"));
    }
    final String password = data.getFirst("Password");
    final String authToken = new Md5Hash(password).toString();
    final String role = data.getFirst("Role");
    final StringBuilder buffer = new StringBuilder();
    buffer.append("/2012-04-24/Accounts/").append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    return new Account(sid, now, now, emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  @Path("/{accountSid}")
  @DELETE public Response deleteAccount(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Delete:Accounts"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    dao.removeAccount(new Sid(accountSid));
    return ok().build();
  }
  
  @Path("/{accountSid}")
  @GET public Response getAccount(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Accounts"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Account account = findAccount(new Sid(accountSid));
    if(account == null) {
      return status(NOT_FOUND).build();
    } else {
      return ok(xstream.toXML(account), APPLICATION_XML).build();
    }
  }
  
  private Account findAccount(final Sid sid) {
    Account account = dao.getAccount(sid);
    if(account == null) {
      account = dao.getSubAccount(sid);
    }
    return account;
  }
  
  @Consumes(APPLICATION_FORM_URLENCODED)
  @POST public Response putAccount(final MultivaluedMap<String, String> data) {
    final Subject subject = SecurityUtils.getSubject();
    Account account = null;
    try { account = createFrom(data); } catch(final NullPointerException exception) {
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    if(subject.hasRole("Administrator")) {
      dao.addAccount(account);
    } else if(subject.isPermitted("RestComm:Create:Accounts")) {
      final Sid accountSid = new Sid((String)subject.getPrincipal());
      dao.addSubAccount(accountSid, account);
    } else {
      return status(UNAUTHORIZED).build();
    }
    return status(CREATED).type(APPLICATION_XML).entity(xstream.toXML(account)).build();
  }
  
  @Path("/{accountSid}")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @PUT public Response updateAccount(@PathParam("accountSid") String accountSid,
      final MultivaluedMap<String, String> data) {
    final Subject subject = SecurityUtils.getSubject();
    final Sid sid = new Sid(accountSid);
    if(subject.hasRole("Administrator")) {
      update(sid, data);
    } else if(subject.getPrincipal().equals(accountSid) && subject.isPermitted("RestComm:Modify:Accounts")) {
      final String friendlyName = data.getFirst("FriendlyName");
      Account.Status status = null;
      try { status = Account.Status.getValueOf(data.getFirst("Status")); }
      catch(final IllegalArgumentException ignored) { }
      update(sid, friendlyName, status);
    } else {
      return status(UNAUTHORIZED).build();
    }
    return ok().build();
  }
  
  private Account update(final Account account, final MultivaluedMap<String, String> data) {
    Account result = account;
    if(data.containsKey("FriendlyName")) {
      result = result.setFriendlyName(data.getFirst("FriendlyName"));
    }
    if(data.containsKey("Type")) {
      result = result.setType(Account.Type.valueOf(data.getFirst("Type")));
    }
    if(data.containsKey("Status")) {
      result = result.setStatus(Account.Status.valueOf(data.getFirst("Status")));
    }
    if(data.containsKey("AuthToken")) {
      result = result.setAuthToken(data.getFirst("AuthToken"));
    }
    return result;
  }
  
  private void update(final Sid sid, final MultivaluedMap<String, String> data) {
    Account account = dao.getAccount(sid);
    if(account != null) {
      dao.updateAccount(update(account, data));
    } else {
      account = dao.getSubAccount(sid);
      if(account != null) {
        dao.updateSubAccount(update(account, data));
      }
    }
  }
  
  private Account update(final Account account, final String friendlyName, final Account.Status status) {
    Account result = account;
    if(friendlyName != null) {
      result = result.setFriendlyName(friendlyName);
    }
    if(status != null) {
      result = result.setStatus(status);
    }
    return result;
  }
  
  private void update(final Sid sid, final String friendlyName, final Account.Status status) {
    Account account = dao.getAccount(sid);
    if(account != null) {
      dao.updateAccount(update(account, friendlyName, status));
    } else {
      account = dao.getSubAccount(sid);
      if(account != null) {
        dao.updateSubAccount(update(account, friendlyName, status));
      }
    }
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
    if(!data.containsKey("EmailAddress")) {
      throw new NullPointerException("Email address can not be null.");
    } else if(!data.containsKey("Password")) {
      throw new NullPointerException("Password can not be null.");
    } else if(!data.containsKey("Role")) {
      throw new NullPointerException("Role can not be null.");
    }
  }
}
