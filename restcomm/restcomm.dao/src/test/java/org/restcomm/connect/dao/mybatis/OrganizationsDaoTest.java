package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.OrganizationsDao;
import org.restcomm.connect.dao.entities.Organization;

import junit.framework.Assert;

public class OrganizationsDaoTest extends DaoTest {
    private static MybatisDaoManager manager;

    public OrganizationsDaoTest() {
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
    public void addOrganizationsTest() throws IllegalArgumentException, URISyntaxException {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Sid sid = Sid.generate(Sid.Type.ORGANIZATION);
        dao.addOrganization(new Organization(sid, "test.restcomm.com", new DateTime(), new DateTime(), Organization.Status.ACTIVE));
        Organization organization = dao.getOrganization(sid);
        Assert.assertNotNull("Organization not found",organization);
        Assert.assertNotNull(organization.getSid());
    }

    @Test
    public void readOrganization() {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Organization organization = dao.getOrganization(new Sid("ORafbe225ad37541eba518a74248f0ac4d"));
        Assert.assertNotNull("Organization not found",organization);
    }

    @Test
    public void readOrganizationByDomainDomain() {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Organization organization = dao.getOrganizationByDomainName("test2.restcomm.com");
        Assert.assertNotNull("Organization not found",organization);
        organization = dao.getOrganizationByDomainName("test2.restcomm.com.");
        Assert.assertNotNull("Organization not found",organization);
    }

}
