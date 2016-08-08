/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.identity;

import junit.framework.Assert;
import org.apache.shiro.authz.SimpleRole;
import org.junit.Test;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;

import java.util.HashMap;

/**
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
public class IdentityContextTest {

    @Test
    public void testConstructors() {
        RestcommRoles roles = new RestcommRoles(new HashMap<String,SimpleRole>());
        IdentityContext context = new IdentityContext(roles);
        Assert.assertEquals(roles,context.restcommRoles);
        Assert.assertNull(context.authServerUrl);
        Assert.assertNull(context.realmName);
        Assert.assertNull(context.realmKey);
        Assert.assertNull(context.dao);

        // TODO fix dao dependency with mock or other way
        /*
        context = new IdentityContext(roles, "name", "publickey", "url", null );
        Assert.assertEquals(roles,context.restcommRoles);
        Assert.assertEquals("name", context.realmName);
        Assert.assertEquals("publickey", context.realmKey);
        Assert.assertEquals("url", context.authServerUrl);
        Assert.assertEquals(null, context.dao); // could not create a mock for this dao in a central location to be used by other tests as well
        */
    }
}
