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

package org.restcomm.connect.extension.mock;

import java.util.Calendar;
import javax.servlet.ServletContext;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.extension.api.ExtensionConfiguration;
import static org.restcomm.connect.extension.api.ExtensionConfiguration.configurationType.JSON;
import org.restcomm.connect.extension.api.ExtensionRequest;
import org.restcomm.connect.extension.api.ExtensionResponse;

/**
 *
 * @author
 */


public class DBExtensionMockTest {

    private static final String EXT_CONF="{}";
    public DBExtensionMockTest() {
    }
    class ExtensionCollaborators {

        ServletContext sCtx = Mockito.mock(ServletContext.class);
        DaoManager daoMng = Mockito.mock(DaoManager.class);
        ExtensionsConfigurationDao extDao = Mockito.mock(ExtensionsConfigurationDao.class);


        public ExtensionCollaborators(String confJson) {
            when(sCtx.getAttribute(DaoManager.class.getName())).
                    thenReturn(daoMng);
            when(daoMng.getExtensionsConfigurationDao()).
                    thenReturn(extDao);
            ExtensionConfiguration extConf = new ExtensionConfiguration(Sid.
                    generate(Sid.Type.EXTENSION_CONFIGURATION),
                    "simple_db",
                    true,
                    confJson,
                    JSON,
                    new DateTime(),new DateTime());
            when(extDao.getConfigurationByName("simple_db")).thenReturn(null);
        }
    }

    @Test
    public void testAllowingExtension() throws Exception {
        ExtensionCollaborators mocks = new ExtensionCollaborators(EXT_CONF);
        DBExtensionMock extension = new DBExtensionMock();
        extension.init(mocks.sCtx);

        final ExtensionRequest far = new ExtensionRequest("accountSid", true);
        ExtensionResponse response = extension.preInboundAction(far);
        Assert.assertTrue(response.isAllowed());
        response = extension.postInboundAction(far);
        Assert.assertTrue(response.isAllowed());

        response = extension.preOutboundAction(far);
        Assert.assertTrue(response.isAllowed());
        response = extension.postOutboundAction(far);
        Assert.assertTrue(response.isAllowed());

        ApiRequest apiReq = new ApiRequest("accountSid", null, ApiRequest.Type.CREATE_SUBACCOUNT);
        response = extension.preApiAction(apiReq);
        Assert.assertTrue(response.isAllowed());
        response = extension.postApiAction(apiReq);
        Assert.assertTrue(response.isAllowed());
    }

}
