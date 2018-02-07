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
package org.restcomm.connect.http.security;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.identity.UserIdentityContext;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Provider
public class SecurityFilter implements ContainerRequestFilter {

    private final Logger logger = Logger.getLogger(SecurityFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    // We return Access-* headers only in case allowedOrigin is present and equals to the 'Origin' header.
    @Override
    public ContainerRequest filter(ContainerRequest cr) {
        final DaoManager storage = (DaoManager) servletRequest.getServletContext().getAttribute(DaoManager.class.getName());
        AccountsDao accountsDao = storage.getAccountsDao();
        UserIdentityContext userIdentityContext = new UserIdentityContext(servletRequest, accountsDao);
        //checkAuthenticatedAccount(userIdentityContext);
        String scheme = cr.getAuthenticationScheme();
        AccountPrincipal aPrincipal = new AccountPrincipal(userIdentityContext);
        cr.setSecurityContext(new RCSecContext(aPrincipal, scheme));
        return cr;
    }
}
