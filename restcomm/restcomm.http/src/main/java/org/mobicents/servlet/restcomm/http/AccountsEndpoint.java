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
package org.mobicents.servlet.restcomm.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.MediaType.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.joda.time.DateTime;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.AccountList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.exceptions.ConstraintViolationException;
import org.mobicents.servlet.restcomm.http.converter.AccountConverter;
import org.mobicents.servlet.restcomm.http.converter.AccountListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.identity.IdentityContext;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakContext;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
public abstract class AccountsEndpoint extends AccountsCommonEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected MutableIdentityConfigurationSet mutableIdentityConfiguration;
    protected IdentityConfigurationSet identityConfiguration;
    protected KeycloakContext keycloakContext;
    protected Gson gson;
    protected XStream xstream;

    public AccountsEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        mutableIdentityConfiguration = RestcommConfiguration.getInstance().getMutableIdentity();
        identityConfiguration = RestcommConfiguration.getInstance().getIdentity();
        keycloakContext = KeycloakContext.getInstance();
        final AccountConverter converter = new AccountConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Account.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new AccountListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }


    /**
     * Create an account out of data. The sid for the new account is random and the account is considered a
     * child of parentAccountSid. EmailAddress property is ignored.
     *
     * @param parentAccountSid
     * @param data
     * @return
     */
    private Account createFrom(final Sid parentAccountSid, final MultivaluedMap<String, String> data) {
        validate(data);
        final DateTime now = DateTime.now();
        //final String emailAddress = data.getFirst("EmailAddress");
        final Sid sid = Sid.generate(Sid.Type.ACCOUNT);

        // use sid as friendly name if missing
        String friendlyName = data.getFirst("FriendlyName");
        if ( org.apache.commons.lang.StringUtils.isEmpty(friendlyName) )
            friendlyName = sid.toString();

        final Account.Type type = Account.Type.FULL;
        Account.Status status = Account.Status.ACTIVE;
        if (data.containsKey("Status")) {
            status = Account.Status.valueOf(data.getFirst("Status"));
        }
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(null)).append("/Accounts/").append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        return new Account(sid, now, now, null, friendlyName, parentAccountSid, type, status, null, null, uri);
    }

    /**
     * If the requested account does not exist and it's the user himself that makes the request, create a new one based
     * on the token information. Of course take into account the configuration too.
     */
    private Account handleMissingAccount( String accountSid, AccessToken token) {
        Account account = null;
        if ( mutableIdentityConfiguration.getAutoImportUsers() ) {
            if ( token.getPreferredUsername().equals(accountSid) ) {
                account = accountFromAccessToken(token);
                accountsDao.addAccount(account);
                logger.info("Automatically created Account '" + account.getSid() + "' and linked to user '" + accountSid + "'" );
            }
        }
        return account;
    }

    protected Response getAccount(final String accountSid, final MediaType responseType) {
        try {
            secure();
            // now load the account that is operated upon
            Sid sid = null;
            Account account = null;
            // Is the account identifier used a sid like "AC5699ea39a9a8c86820ea241e843c221e" ?
            if (Sid.pattern.matcher(accountSid).matches()) {
                try {
                    sid = new Sid(accountSid);
                    account = accountsDao.getAccount(sid);
                } catch (Exception e) {
                    return status(NOT_FOUND).build();
                }

            } else { // If not, try to load by FriendlyName, email etc.
                try {
                    account = accountsDao.getAccount(accountSid);
                    if ( account == null )
                        account = handleMissingAccount(accountSid, getKeycloakAccessToken());
                    sid = account.getSid();
                } catch (Exception e) {
                    return status(NOT_FOUND).build();
                }
            }

            if (account == null) {
                return status(NOT_FOUND).build();
            } else {
                // make sure the logged user can access this account
                secure(account, "RestComm:Read:Accounts");
                if (APPLICATION_XML_TYPE == responseType) {
                    final RestCommResponse response = new RestCommResponse(account);
                    return ok(xstream.toXML(response), APPLICATION_XML).build();
                } else if (APPLICATION_JSON_TYPE == responseType) {
                    return ok(gson.toJson(account), APPLICATION_JSON).build();
                } else {
                    return null;
                }
            }
        } catch ( AuthorizationException e) {
            return status(UNAUTHORIZED).build();
        }
    }

    protected Response deleteAccount(final String operatedSid) {
        final Sid sidToBeRemoved = new Sid(operatedSid);
        try {
            Account removedAccount = accountsDao.getAccount(sidToBeRemoved);
            secure(removedAccount, "RestComm:Delete:Accounts");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        // Prevent removal of Logged account
        if (operatedSid.equalsIgnoreCase(identityContext.getEffectiveAccount().getSid().toString()))
            return status(BAD_REQUEST).build();

        if (accountsDao.getAccount(sidToBeRemoved) == null)
            return status(NOT_FOUND).build();

        accountsDao.removeAccount(sidToBeRemoved);
        return ok().build();
    }

    // Returns (sub-)accounts for logged user. Proper sub-account implementation is still an issue to implement - https://github.com/Mobicents/RestComm/issues/227
    protected Response getAccounts(final MediaType responseType) {
        try {
            secure("RestComm:Read:Accounts");
        } catch(final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Account account = identityContext.getEffectiveAccount(); // uses either oauth token or APIKey specified account
        if (account == null) {
            return status(NOT_FOUND).build();
        } else {
            final List<Account> accounts = new ArrayList<Account>();
            accounts.add(account); // TODO maybe we need to remove this at some point. There is a different API call for retrieving the users own account
            accounts.addAll(accountsDao.getAccounts(account.getSid())); // remember getSid() retrieves the accounts own id while getAccountSid() retrieves the sid of the parent account
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(new AccountList(accounts));
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(accounts), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    /**
     * Create a new restcomm account based on data from request. The following fields are taken into account:
     *
     *  - FriendlyName
     *  - AccountStatue
     *
     *  Note that EmailAddress is ignored
     *
     * @param data
     * @param responseType
     * @return
     */
    protected Response putAccount(final MultivaluedMap<String, String> data, final MediaType responseType) {
        try {
            secure("RestComm:Create:Accounts");
        } catch(final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        final Account parentAccount = identityContext.getEffectiveAccount();
        if ( parentAccount == null )
            return status(NOT_FOUND).build();
        Account account = null; // the newly created account
        try {
            account = createFrom(parentAccount.getSid(), data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        // check if the account that is being created already exists
        if ( accountsDao.getAccount(account.getEmailAddress()) != null )
            return status(CONFLICT).build();

        // set role
        final String selectedRole = data.getFirst("Role");
        final String childRole = getChildRole(parentAccount, selectedRole );
        account = account.setRole(childRole);

        // assign a random authToken for the account
        account = account.setAuthToken(IdentityContext.generateApiKey());

        // store restcomm account
        // note that email_address should be empty since it is ignored in createFrom()
        try {
            accountsDao.addAccount(account);
        } catch (ConstraintViolationException e) {
            return status(CONFLICT).build();
        }
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(account), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(account);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    private Account update(final Account account, final MultivaluedMap<String, String> data) {
        Account result = account;
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("Status")) {
            result = result.setStatus(Account.Status.getValueOf(data.getFirst("Status")));
        } else {
            result = result.setStatus(Account.Status.ACTIVE);
        }
        return result;
    }

    protected Response updateAccount(final String accountSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        final Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);

        if (account == null) {
            return status(NOT_FOUND).build();
        } else {
            // make sure the logged user can modify accounts AND that he owns (or is a parent of) this account
            secure(account, "RestComm:Modify:Accounts");
            // update the model
            account = update(account, data);
            // if all goes well, persist the updated account in database
            try {
                accountsDao.updateAccount(account);
            } catch (ConstraintViolationException e) {
                return status(CONFLICT).build();
            }

            if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(account), APPLICATION_JSON).build();
            } else if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(account);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else {
                return null;
            }
        }
    }

    /**
     * Links a Restcomm account with a keycloak user using the Account.emailAddress property.
     * Both local account records and authorization server user roles are updated.
     *
     * @param restcommAccount
     * @param username
     */
    protected Outcome linkAccountToUser(Account restcommAccount, String username) {
        if ( !validateUsername(username) )
            return Outcome.BAD_INPUT;
        if ( org.apache.commons.lang.StringUtils.isEmpty(restcommAccount.getEmailAddress()) ) {
            RestcommIdentityApi api = new RestcommIdentityApi(identityContext, RestcommConfiguration.getInstance().getIdentity(), RestcommConfiguration.getInstance().getMutableIdentity());
            if ( ! api.inviteUser(username) ) // assign roles
                return Outcome.NOT_FOUND;
            restcommAccount = restcommAccount.setEmailAddress(username);
            try {
                accountsDao.updateAccount(restcommAccount);
            } catch (ConstraintViolationException e) {
                return Outcome.CONFLICT;
            }
            return Outcome.OK;
        } else {
            // the Account is already mapped. Maybe unmap it first ?
            return Outcome.CONFLICT;
        }
    }

    /**
     * Breaks the link between an account and a user by clearing EmailAddress 'reference' property.
     * The user KEEPS the roles that grant him access though.
     *
     * @param restcommAccount
     * @return
     */
    protected Response unlinkAccountFromUser(Account restcommAccount) {
        restcommAccount = restcommAccount.setEmailAddress(null);
        accountsDao.updateAccount(restcommAccount);
        return Response.ok().build();
    }

    protected Response clearAccountKey(Account restcommAccount) {
        restcommAccount = restcommAccount.setAuthToken(null);
        accountsDao.updateAccount(restcommAccount);
        return Response.ok().build();
    }

    protected Response assignApikey(Account restcommAccount) {
        if ( ! org.apache.commons.lang.StringUtils.isEmpty(restcommAccount.getAuthToken()) )
            return Response.status(CONFLICT).build();
        String key = IdentityContext.generateApiKey();
        restcommAccount = restcommAccount.setAuthToken(key);
        accountsDao.updateAccount(restcommAccount);
        return Response.ok().build();
    }

    protected Outcome createUser(String username, String friendlyName, String tempPassword) {
        if ( !validateUsername(username) )
            return Outcome.BAD_INPUT;
        UserEntity user = new UserEntity(username,null, friendlyName, null, tempPassword);
        RestcommIdentityApi api = new RestcommIdentityApi(identityContext,identityConfiguration,mutableIdentityConfiguration );
        return api.createUser(user);
    }

    // Validates an username (EmailAddress) before mapping an account to it.
    // TODO - validation rules may include checks whether this address is @deployment.domain
    protected boolean validateUsername(String username) {
        // TODO - implement...
        return true;
    }

    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
        // For SSO these fields are not required or are not used
        //if (!data.containsKey("EmailAddress")) {
        //    throw new NullPointerException("Email address can not be null.");
        //} /*else if (!data.containsKey("Password")) {
        //    throw new NullPointerException("Password can not be null.");
        //}*/
    }

    // calculates the role for a sub-account based on parent account, user selection and logged user. For now it always creates a Developer
    private String getChildRole(Account parentAccount, String selectedRole ) {
        // TODO add a proper implementation
        return "Developer";
    }

}
