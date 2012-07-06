package org.mobicents.servlet.sip.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

@Path("/Accounts.json")
@ThreadSafe public final class AccountsJsonEndpoint extends AccountsEndpoint {
  public AccountsJsonEndpoint() {
    super();
  }
  
  @GET public Response getAccounts() {
    return getAccounts(APPLICATION_JSON_TYPE);
  }
  
  @Consumes(APPLICATION_FORM_URLENCODED)
  @POST public Response putAccount(final MultivaluedMap<String, String> data) {
    return putAccount(data, APPLICATION_JSON_TYPE);
  }
}
