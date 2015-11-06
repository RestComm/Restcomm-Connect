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

package org.mobicents.servlet.restcomm.identity.migration;

import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class IdentityMigrationTool {

    private AccountsDao accountsDao;
    private RestcommIdentityApi identityApi;
    private boolean inviteExistingUsers;

    public IdentityMigrationTool(AccountsDao dao, RestcommIdentityApi identityApi, boolean inviteExisting) {
        super();
        this.accountsDao = dao;
        this.identityApi = identityApi;
        this.inviteExistingUsers = inviteExisting;
    }

    public void migrateUsers() {
       // RestcommIdentityApi api = new RestcommIdentityApi(authurl, username, password)
    }

    public boolean migrateAccount(String accountSid) {
        Account account = accountsDao.getAccount(new Sid(accountSid));
        if (StringUtils.isEmpty(account.getEmailAddress()))
            report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + ") - account has no email address and won't be migrated");
        else {
            String password = account.getAuthToken(); // use auth_token as password for the user. Other options?
            UserEntity user = new UserEntity(account.getEmailAddress(), account.getEmailAddress(), account.getFriendlyName(), null, password );
            Outcome outcome = identityApi.createUser(user);
            if ( outcome == Outcome.CONFLICT ) {
                // the user is already there. Check the policy and act accordingly
                if (this.inviteExistingUsers) {
                    if ( ! identityApi.inviteUser(account.getEmailAddress()) )
                        report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") failed: Invitation to existing user failed.");
                    else {
                        report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") SUCCESS");
                        return true;
                    }
                } else
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") failed: User already exists and policy does not allow invitations");
            } else
            if (outcome == Outcome.OK) {
                if (!identityApi.inviteUser(account.getEmailAddress()))
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") failed: New user was created but invitation failed" );
                else {
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") SUCCESS");
                    return true;
                }
            } else
                report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") failed: Could not create user");
        }
        return false;
    }

    private void report(String message) {

    }

}
