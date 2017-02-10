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

package org.restcomm.connect.rvd.http.resources;

import junit.framework.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RvdControllerTest {

    @Test
    public void testAppIdExtraction() throws URISyntaxException {
        RvdController controller = new RvdController();
        String appId = controller.extractAppIdFromPath("apps/AP73926e7113fa4d95981aa96b76eca854/bla/bla");
        Assert.assertEquals("AP73926e7113fa4d95981aa96b76eca854",appId);
        appId = controller.extractAppIdFromPath("apps/AP73926e7113fa4d95981aa96b76eca854");
        Assert.assertEquals("AP73926e7113fa4d95981aa96b76eca854",appId);
        appId = controller.extractAppIdFromPath("apps/AP73926e7113fa4d95981aa96b76eca854-a");
        Assert.assertNull(appId);
    }
}
