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

package org.mobicents.servlet.restcomm.rvd.restcomm;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.rvd.TestUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Orestis Tsakiridis
 */
public class RestcommClientTest {

    private static URI fallbackUri;

    @BeforeClass
    public static void init() {
        fallbackUri = URI.create("http://123.123.123.123:7070");
        TestUtils.initRvdConfiguration();
    }

    @Test(expected=RestcommClient.RestcommClientInitializationException.class)
    public void exceptionThrownWhenNoCredentialsCanBeDetermined() throws RestcommClient.RestcommClientInitializationException, URISyntaxException {
        RestcommClient client = new RestcommClient(fallbackUri,null,null);
    }

}
