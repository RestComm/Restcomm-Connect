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

package org.restcomm.connect.http.rcmlserver;

import org.junit.Test;
import org.restcomm.connect.commons.common.http.SslMode;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.configuration.sets.impl.MainConfigurationSetImpl;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Sid;
import org.restcomm.connect.http.client.RcmlserverApi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverApiTest {
    @Test
    public void notifyAccountsRemoval() throws URISyntaxException, InterruptedException {

        MainConfigurationSet mainConfig = new MainConfigurationSetImpl(SslMode.strict,5000,false,null,null,false);
        List<Account> closedAccounts = new ArrayList<Account>();
        closedAccounts.add(createAccount("AC00000000000000000000000000000001"));
        closedAccounts.add(createAccount("AC00000000000000000000000000000002"));
        closedAccounts.add(createAccount("AC00000000000000000000000000000003"));
        closedAccounts.add(createAccount("AC00000000000000000000000000000004"));
        RcmlserverApi api = new RcmlserverApi(mainConfig,new URI("http://192.168.2.4:8095/restcomm-rvd/services"));
        Thread thread = api.notifyAccountsRemovalAsync(closedAccounts,"administrator@company.com", "RestComm");
        thread.join();
    }

    private Account createAccount(String sid) {
        Account.Builder builder = Account.builder();
        builder.setSid(new Sid(sid));
        return builder.build();
    }

}
