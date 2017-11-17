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
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberFilter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class IncomingPhoneNumbersRetrievalTest extends DaoTest {
    private static MybatisDaoManager manager;

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("IncomingPhoneNumbersRetrievalTest");
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
    public void retrieveNumbersForApplication() {
        IncomingPhoneNumbersDao dao = manager.getIncomingPhoneNumbersDao();
        // application AP73926e7113fa4d95981aa96b76eca854 is bound with 3 numbers
        IncomingPhoneNumberFilter filter = new IncomingPhoneNumberFilter("ACae6e420f425248d6a26948c17a9e2acf", null, null, "AP73926e7113fa4d95981aa96b76eca854", null, null, 100, 0);
        List<IncomingPhoneNumber> numbers = dao.getIncomingPhoneNumbersByFilter(filter);
        Assert.assertEquals(3, numbers.size());
        // application AP00000000000000000000000000000004 is bound with no numbers
        filter = new IncomingPhoneNumberFilter("ACae6e420f425248d6a26948c17a9e2acf", null, null, "AP00000000000000000000000000000004", null, null, 100, 0);
        numbers = dao.getIncomingPhoneNumbersByFilter(filter);
        Assert.assertEquals(0, numbers.size());
    }

    @Test
    public void countTotalNumbersForApplication() {
        IncomingPhoneNumbersDao dao = manager.getIncomingPhoneNumbersDao();
        IncomingPhoneNumberFilter filter = new IncomingPhoneNumberFilter("ACae6e420f425248d6a26948c17a9e2acf", null, null, "AP73926e7113fa4d95981aa96b76eca854", null, null, 100, 0);
        int total = dao.getTotalIncomingPhoneNumbers(filter);
        Assert.assertEquals(3, total);
    }

}
