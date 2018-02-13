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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.ProfilesDao;
import org.restcomm.connect.dao.entities.Profile;

import junit.framework.Assert;

public class ProfilesDaoTest extends DaoTest {
    private static MybatisDaoManager manager;
    private static final String jsonProfile = "{ \"FACEnablement\": { \"destinations\": { \"allowedPrefixes\": [\"US\", \"Canada\"] }, \"outboundPSTN\": { }, \"inboundPSTN\": { }, \"outboundSMS\": { }, \"inboundSMS\": { } }}";
    private static final String jsonUpdateProfile = "{ \"FACEnablement\": { \"destinations\": { \"allowedPrefixes\": [\"US\", \"Canada\", \"Pakitsan\"] }, \"outboundPSTN\": { }, \"inboundPSTN\": { }, \"outboundSMS\": { }, \"inboundSMS\": { } }}";

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
    public void ProfileCRUDTest() throws IllegalArgumentException, URISyntaxException, IOException, SQLException {
        ProfilesDao dao = manager.getProfilesDao();
        Profile profile = new Profile(Sid.generate(Sid.Type.PROFILE).toString(), jsonProfile, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());

        // Add Profile
        dao.addProfile(profile);

        // Read Profile
        Profile resultantProfile = dao.getProfile(profile.getSid());
        Assert.assertNotNull(resultantProfile);
        Assert.assertEquals(profile.getSid(), resultantProfile.getSid());
//        Assert.assertTrue(Arrays.equals(profile.getProfileDocument(), resultantProfile.getProfileDocument()));

        //Read Profile List
        List<Profile> profilelist = dao.getAllProfiles();
        Assert.assertNotNull(profilelist);
        Assert.assertEquals(1, profilelist.size());

        // Update Profile
        Profile updatedProfile = profile.setProfileDocument(jsonUpdateProfile);
        dao.updateProfile(updatedProfile);


        resultantProfile = dao.getProfile(updatedProfile.getSid());
        Assert.assertNotNull(resultantProfile);
        Assert.assertEquals(updatedProfile.getSid(), resultantProfile.getSid());
//        Assert.assertTrue(Arrays.equals(updatedProfile.getProfileDocument(), resultantProfile.getProfileDocument()));

        // Delete Profile
        dao.deleteProfile(updatedProfile.getSid().toString());

        resultantProfile = dao.getProfile(profile.getSid());
        Assert.assertNull(resultantProfile);
    }
}
