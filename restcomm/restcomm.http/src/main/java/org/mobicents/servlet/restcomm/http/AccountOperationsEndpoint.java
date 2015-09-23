package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.Response.Status.CONFLICT;

import javax.annotation.PostConstruct;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.IdentityContext;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;
import org.apache.commons.lang.StringUtils;

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
    public Response linkAccount(@PathParam("accountSid") String accountSid, @FormParam("username") String username, @FormParam("create") String create, @FormParam("friendly_name") String friendly_name, @FormParam("password") String password) {
        // TODO - access control
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        if ( "true".equals(create) && !StringUtils.isEmpty(username) ) {
            Outcome create_outcome = createUser(username, friendly_name, password);
            if ( create_outcome == Outcome.OK )
                return toResponse(linkAccountToUser(account, username));
            else
                return toResponse(create_outcome);
        } else {
            return toResponse(linkAccountToUser(account, username));
        }
    }

    @DELETE
    @Path("/link")
    public Response unlinkAccount(@PathParam("accountSid") String accountSid) {
     // TODO - access control
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return unlinkAccountFromUser(account);
    }

    @DELETE
    @Path("/key")
    public Response removeApikey(@PathParam("accountSid") String accountSid) {
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return clearAccountKey(account);
    }

    @GET
    @Path("/key/assign")
    public Response assignApikey(@PathParam("accountSid") String accountSid) {
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return assignApikey(account);
    }


    /**
     * Links a Restcomm account with a keycloak user using the Account.emailAddress property.
     * @param restcommAccount
     * @param username
     */
    private Outcome linkAccountToUser(Account restcommAccount, String username) {
        if ( !validateUsername(username) )
            return Outcome.BAD_INPUT;
        if ( org.apache.commons.lang.StringUtils.isEmpty(restcommAccount.getEmailAddress()) ) {
            RestcommIdentityApi api = new RestcommIdentityApi(identityContext, identityConfigurator);
            if ( ! api.inviteUser(username) ) // assign roles
                return Outcome.NOT_FOUND;
            restcommAccount = restcommAccount.setEmailAddress(username);
            accountsDao.updateAccount(restcommAccount);
            return Outcome.OK;
        } else {
            // the Account is already mapped. Maybe unmap it first ?
            return Outcome.CONFLICT;
        }
    }

    private Outcome createUser(String username, String friendlyName, String tempPassword) {
        if ( !validateUsername(username) )
            return Outcome.BAD_INPUT;
        UserEntity user = new UserEntity(username,null, friendlyName, null, tempPassword);
        RestcommIdentityApi api = new RestcommIdentityApi(identityContext, identityConfigurator);
        return api.createUser(user);
    }

    private Response unlinkAccountFromUser(Account restcommAccount) {
        restcommAccount = restcommAccount.setEmailAddress(null);
        accountsDao.updateAccount(restcommAccount);
        return Response.ok().build();
    }

    private Response clearAccountKey(Account restcommAccount) {
        restcommAccount = restcommAccount.setAuthToken(null);
        accountsDao.updateAccount(restcommAccount);
        return Response.ok().build();
    }

    private Response assignApikey(Account restcommAccount) {
        if ( ! org.apache.commons.lang.StringUtils.isEmpty(restcommAccount.getAuthToken()) )
            return Response.status(CONFLICT).build();
        String key = IdentityContext.generateApiKey();
        restcommAccount = restcommAccount.setAuthToken(key);
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
