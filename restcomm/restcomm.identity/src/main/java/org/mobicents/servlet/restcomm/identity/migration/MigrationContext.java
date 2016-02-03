/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.identity.migration;

import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.dao.AccountsDao;

import javax.servlet.ServletContext;

/**
 * A wrapper class that contains all information required to perform a migration.
 *
 * @author Orestis Tsakiridis
 */
public class MigrationContext {
    private final IdentityConfigurationSet identityConfig;
    private final MutableIdentityConfigurationSet mutableIdentityConfig;
    private final RestcommConfiguration restcommConfiguration;
    private final ServletContext servletContext;
    private final AccountsDao accountsDao;
    private final boolean bootstrapping;

    public MigrationContext(IdentityConfigurationSet identityConfig, MutableIdentityConfigurationSet mutableIdentityConfig, RestcommConfiguration restcommConfiguration, ServletContext servletContext, AccountsDao accountsDao, boolean bootstrapping) {
        this.identityConfig = identityConfig;
        this.mutableIdentityConfig = mutableIdentityConfig;
        this.restcommConfiguration = restcommConfiguration;
        this.servletContext = servletContext;
        this.accountsDao = accountsDao;
        this.bootstrapping = bootstrapping;
    }

    public IdentityConfigurationSet getIdentityConfig() {
        return identityConfig;
    }

    public MutableIdentityConfigurationSet getMutableIdentityConfig() {
        return mutableIdentityConfig;
    }

    public RestcommConfiguration getRestcommConfiguration() {
        return restcommConfiguration;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public AccountsDao getAccountsDao() {
        return accountsDao;
    }

    public boolean isBootstrapping() { return bootstrapping; }
}
