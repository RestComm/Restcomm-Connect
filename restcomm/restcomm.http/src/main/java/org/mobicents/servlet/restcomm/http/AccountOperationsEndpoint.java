package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;

import javax.annotation.PostConstruct;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

@Path("/Accounts/{accountSid}/operations")
@ThreadSafe
public class AccountOperationsEndpoint extends SecuredEndpoint {

    public AccountOperationsEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
    }

    @POST
    @Path("/link")
    public Response linkAccount(@PathParam("accountSid") String accountSid, @FormParam("username") String username) {
        // TODO - access control
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return linkAccountToUser(account, username);
    }

    @DELETE
    @Path("/link")
    public Response unlinkAccount(@PathParam("accountSid") String accountSid, @FormParam("username") String username) {
     // TODO - access control
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return unlinkAccountFromUser(account);
    }

    /**
     * Links a Restcomm account with a keycloak user using the Account.emailAddress property.
     * @param restcommAccount
     * @param username
     */
    private Response linkAccountToUser(Account restcommAccount, String username) {
        if ( !validateUsername(username) )
            return Response.status(BAD_REQUEST).build();
        if ( org.apache.commons.lang.StringUtils.isEmpty(restcommAccount.getEmailAddress()) ) {
            restcommAccount = restcommAccount.setEmailAddress(username);
            accountsDao.updateAccount(restcommAccount);
            return Response.ok().build();
        } else {
            // the Account is already mapped. Maybe unmap it first ?
            return Response.status(CONFLICT).build();
        }
    }

    private Response unlinkAccountFromUser(Account restcommAccount) {
        restcommAccount = restcommAccount.setEmailAddress(null);
        accountsDao.updateAccount(restcommAccount);
        return Response.ok().build();
    }

    // Validates an username (EmailAddress) before mapping an account to it.
    // TODO - validation rules may include checks whether this address is @deployment.domain
    private boolean validateUsername(String username) {
        // TODO - implement...
        return true;
    }

}
