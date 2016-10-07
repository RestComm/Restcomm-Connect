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

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverApiTest {
    @Test
    void notifyAccountRemoval() {
        /*
        MainConfigurationSet mainConfig = new MainConfigurationSetImpl(SslMode.strict,5000,false,null,null,false);
        RcmlserverApi api = new RcmlserverApi("http://this:8080/restcomm-rvd/services", mainConfig);
        Account.Builder builder = Account.builder();
        builder.setSid(new Sid("AC00000000000000000000000000000000"));
        builder.setAuthToken("secret");
        Account removedAccount = builder.build();
        api.notifyAccountRemovalAsync(removedAccount,"administrator@company.com", "RestComm");
        */
    }
}
