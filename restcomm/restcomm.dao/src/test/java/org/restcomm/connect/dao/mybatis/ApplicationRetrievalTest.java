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

import junit.framework.Assert;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.ApplicationsDao;
import org.restcomm.connect.dao.entities.Application;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ApplicationRetrievalTest extends DaoTest {
    private static MybatisDaoManager manager;

    public ApplicationRetrievalTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("applicationRetrievalTest");
        String mybatisFilesPath = getClass().getResource("/applicationsDao").getFile();
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
    public void retrieveApplications() {
        ApplicationsDao dao = manager.getApplicationsDao();
        List<Application> apps = dao.getApplications(new Sid("ACae6e420f425248d6a26948c17a9e2acf"));
        Assert.assertEquals(5, apps.size());
    }

    /**
     * PN00000000000000000000000000000001 is bound to AP73926e7113fa4d95981aa96b76eca854 (sms)
     * PN00000000000000000000000000000002 is bound to AP73926e7113fa4d95981aa96b76eca854 (both sms+ussd)
     * PN00000000000000000000000000000003 is bound to AP00000000000000000000000000000005. The number belong sto other account
     * AP00000000000000000000000000000006 belongs to other account
     */
    @Test
    public void retrieveApplicationsAndTheirNumbers() {
        ApplicationsDao dao = manager.getApplicationsDao();
        List<Application> apps = dao.getApplicationsWithNumbers(new Sid("ACae6e420f425248d6a26948c17a9e2acf"));

        Assert.assertEquals(5, apps.size());
        Assert.assertNotNull(searchApplicationBySid(new Sid("AP73926e7113fa4d95981aa96b76eca854"),apps).getNumbers());
        // applications bound with many numbers are property returned
        Assert.assertEquals("Three (3) numbers should be bound to this application", 3, searchApplicationBySid(new Sid("AP73926e7113fa4d95981aa96b76eca854"),apps).getNumbers().size());
        // applications bound with no numbers are properly returned
        Assert.assertNull(searchApplicationBySid(new Sid("AP00000000000000000000000000000004"),apps).getNumbers());
        // applications bound to numbers that belong to different account should not be returned (for now at least)
        Assert.assertNull(searchApplicationBySid(new Sid("AP00000000000000000000000000000005"),apps).getNumbers());

    }

    private Application searchApplicationBySid(Sid sid, List<Application> apps) {
        if (apps != null) {
            int i = 0;
            while (i < apps.size()) {
                if (apps.get(i).getSid().equals(sid))
                    return apps.get(i);
                i++;
            }
        }
        return null; // nothing found

    }
}
