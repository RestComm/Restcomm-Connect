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
package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.OrganizationsDao;
import org.restcomm.connect.dao.ProfilesDao;
import org.restcomm.connect.dao.entities.Organization;
import org.restcomm.connect.dao.entities.Profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;

import junit.framework.Assert;

public class ProfilesDaoTest extends DaoTest {
    private static MybatisDaoManager manager;
    private static final String jsonProfile = "{ \"FACEnablement\": { \"destinations\": { \"allowedPrefixes\": [\"US\", \"Canada\"] }, \"outboundPSTN\": { }, \"inboundPSTN\": { }, \"outboundSMS\": { }, \"inboundSMS\": { } }}";

    public ProfilesDaoTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("organizationsTest");
        String mybatisFilesPath = getClass().getResource("/organizationsDao").getFile();
        setupSandbox(mybatisFilesPath, sandboxRoot);

        String mybatisXmlPath = sandboxRoot.getPath() + "/mybatis_updated.xml";
        final InputStream data = new FileInputStream(mybatisXmlPath);
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() throws Exception {
        manager.shutdown();
        removeTempDir(sandboxRoot.getAbsolutePath());
    }

    @Test
    public void ProfileCRUDTest() throws IllegalArgumentException, URISyntaxException, IOException {
        ProfilesDao dao = manager.getProfilesDao();
        Profile profile = new Profile(Sid.generate(Sid.Type.PROFILE).toString(), jsonProfile.getBytes(), Calendar.getInstance().getTime(), Calendar.getInstance().getTime());

        // Add Profile
        dao.addProfile(profile);

        // Read Profile
        Profile resultantProfile = dao.getProfile(profile.getSid());
        Assert.assertNotNull(resultantProfile);
        Assert.assertEquals(profile.getSid(), resultantProfile.getSid());
        Assert.assertEquals(profile.getProfileDocument(), resultantProfile.getProfileDocument());

        // Update Profile

        // Delete Profile
    }
}
