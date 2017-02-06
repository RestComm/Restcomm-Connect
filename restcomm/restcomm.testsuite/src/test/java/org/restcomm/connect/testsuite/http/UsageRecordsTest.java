/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.restcomm.connect.testsuite.http;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.google.gson.JsonElement;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommUsageRecordsTool;

import java.net.URL;

//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertNull;
//import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:abdulazizali@acm.org">abdulazizali77</a>
 */

@RunWith(Arquillian.class)
public class UsageRecordsTest {
    private static Logger logger = Logger.getLogger(UsageRecordsTest.class);

    private static final String version = Version.getVersion();
    private static final String revision = Version.getRevision();

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Test(expected = UniformInterfaceException.class)
    public void getUsageRecordsList() {
        //NB: currently unimplemented
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecords(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", "", true);
        logger.info(firstPage);
    }

    @Test(expected = UniformInterfaceException.class)
    public void getUsageRecordsListCategoryCalls() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsDaily(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "calls", true);
        logger.info(firstPage);
    }

    @Test(expected = UniformInterfaceException.class)
    public void getUsageRecordsListCategorySms() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsDaily(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "sms", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsDaily() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsDaily(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsMonthly() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsMonthly(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsYearly() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsYearly(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsAlltime() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsAllTime(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsToday() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsToday(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsYesterday() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsYesterday(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsThisMonth() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsThisMonth(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

    @Test
    public void getUsageRecordsLastMonth() {
        JsonElement firstPage = RestcommUsageRecordsTool.getInstance()
                .getUsageRecordsLastMonth(deploymentUrl.toString(), adminAccountSid, adminAuthToken, "", true);
        logger.info(firstPage);
    }

//    // Regression tests
//    @Test
//    public void getUsageRecordsUri() {
//
//    }
//
//    @Test
//    public void getUsageRecordsSubresourceUris() {
//
//    }
//
//    @Test
//    public void getUsageRecordsUsageCountPrice() {
//
//    }

    @Deployment(name = "UsageRecordsTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        WebArchive archive = ShrinkWrap
                .create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven
                .resolver()
                .resolve(
                        "org.restcomm:restcomm-connect.application:war:"
                                + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        //TODO: add new data script with additional records
        archive.addAsWebInfResource("restcomm_with_Data.script",
                "data/hsql/restcomm.script");
        return archive;
    }
}