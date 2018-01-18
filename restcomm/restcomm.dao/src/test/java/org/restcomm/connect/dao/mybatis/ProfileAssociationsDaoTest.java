package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.ProfileAssociationsDao;
import org.restcomm.connect.dao.entities.ProfileAssociation;

import junit.framework.Assert;

public class ProfileAssociationsDaoTest extends DaoTest {
    private static MybatisDaoManager manager;

    public ProfileAssociationsDaoTest() {
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
        ProfileAssociationsDao dao = manager.getProfileAssociationsDao();
        //TODO: update profile sid type below after merge from master
        Sid profileSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid targetSid = Sid.generate(Sid.Type.ACCOUNT);
        ProfileAssociation profileAssociation = new ProfileAssociation(profileSid, targetSid, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        dao.addProfileAssociation(profileAssociation);
        ProfileAssociation resultantProfileAssociation = dao.getProfileAssociationByTargetSid(targetSid.toString());
        Assert.assertNotNull(resultantProfileAssociation);
    }

}
