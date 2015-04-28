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

import java.net.URI;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.shiro.ShiroResources;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@NotThreadSafe
public abstract class AbstractEndpoint {
    private Logger logger = Logger.getLogger(AbstractEndpoint.class);
    private String defaultApiVersion;
    protected Configuration configuration;
    protected String baseRecordingsPath;

    @Context
    HttpServletRequest request;
    protected static RestcommRoles restcommRoles;


    public AbstractEndpoint() {
        super();
    }

    protected void init(final Configuration configuration) {
        final String path = configuration.getString("recordings-path");
        baseRecordingsPath = StringUtils.addSuffixIfNotPresent(path, "/");
        defaultApiVersion = configuration.getString("api-version");
        ShiroResources shiroResources = ShiroResources.getInstance();
        restcommRoles = shiroResources.get(RestcommRoles.class);
    }

    protected String getApiVersion(final MultivaluedMap<String, String> data) {
        String apiVersion = defaultApiVersion;
        if (data != null && data.containsKey("ApiVersion")) {
            apiVersion = data.getFirst("ApiVersion");
        }
        return apiVersion;
    }

    protected PhoneNumber getPhoneNumber(final MultivaluedMap<String, String> data) {
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneNumberUtil.parse(data.getFirst("PhoneNumber"), "US");
        } catch (final NumberParseException ignored) {
        }
        return phoneNumber;
    }

    protected String getMethod(final String name, final MultivaluedMap<String, String> data) {
        String method = "POST";
        if (data.containsKey(name)) {
            method = data.getFirst(name);
        }
        return method;
    }

    protected Sid getSid(final String name, final MultivaluedMap<String, String> data) {
        Sid sid = null;
        if (data.containsKey(name)) {
            sid = new Sid(data.getFirst(name));
        }
        return sid;
    }

    protected URI getUrl(final String name, final MultivaluedMap<String, String> data) {
        URI uri = null;
        if (data.containsKey(name)) {
            uri = URI.create(data.getFirst(name));
        }
        return uri;
    }

    protected boolean getHasVoiceCallerIdLookup(final MultivaluedMap<String, String> data) {
        boolean hasVoiceCallerIdLookup = false;
        if (data.containsKey("VoiceCallerIdLookup")) {
            final String value = data.getFirst("VoiceCallerIdLookup");
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return hasVoiceCallerIdLookup;
    }

    protected void secure(final Account account, final String permission) throws AuthorizationException {
        secureKeycloak(account, "domain:" + permission, getKeycloakAccessToken()); // add the 'domain:' prefix too
        //final Subject subject = SecurityUtils.getSubject();
        //final Sid accountSid = account.getSid();
        //if (account.getStatus().equals(Account.Status.ACTIVE) && (subject.hasRole("Administrator") || (subject.getPrincipal().equals(accountSid) && subject.isPermitted(permission)))) {
        //    return;
        //} else {
        //    throw new AuthorizationException();
        //}
    }

    protected void secureKeycloak(final Account account, final String neededPermissionString, final AccessToken accessToken) {
        Set<String> roleNames;
        try {
            roleNames = accessToken.getRealmAccess().getRoles();
        } catch (NullPointerException e) {
            throw new UnauthorizedException("No access token present or no roles in it");
        }

        String message = "has the following roles: ";
        for (String rolename: roleNames) {
            message += rolename + ", ";
        }
        logger.info(message);

        //String neededPermissionString = "domain:restcomm:read:accounts";
        WildcardPermissionResolver resolver = new WildcardPermissionResolver();
        Permission neededPermission = resolver.resolvePermission(neededPermissionString);
        //Permission neededPermission = new DomainPermission(neededPermissionString); // Developer has: RestComm:*:Calls

        // build the authorization token
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(roleNames);

        // check the neededPermission against all roles of the user
        for (String roleName: roleNames) {
            SimpleRole simpleRole = restcommRoles.getRole(roleName);
            if ( simpleRole == null)
                logger.info("Cannot map keycloak role '" + roleName + "' to local restcomm configuration. Ignored." );
            else {
                // simpleRole, neededPermission
                logger.info("checking role " + roleName);

                Set<Permission> permissions = simpleRole.getPermissions();
                // check the permissions one by one

                for (Permission permission: permissions) {
                    logger.info("Testing " + neededPermissionString + " against " + permission.toString() );
                    if (permission.implies(neededPermission)) {
                        logger.info("permission granted!");
                        return;
                    }

                }

                logger.info("role " + roleName + " does not allow " + neededPermissionString);
            }
        }
        logger.info("No granting role/permission found. The request won't be permitted");
        throw new AuthorizationException();
    }

    // uses keycloak token
    protected String getLoggedUsername() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if (session.getToken() != null) {
            return session.getToken().getPreferredUsername();
        }
        return null;
    }

    protected AccessToken getKeycloakAccessToken() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = session.getToken();
        return accessToken;
    }



}
