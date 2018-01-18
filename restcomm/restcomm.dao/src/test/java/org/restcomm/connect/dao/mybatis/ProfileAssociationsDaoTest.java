package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.List;

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
    public void profileAssociationsCRUDTest() throws IllegalArgumentException, URISyntaxException {
        ProfileAssociationsDao dao = manager.getProfileAssociationsDao();
        //TODO: update profile sid type below after merge from master
        Sid profileSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid targetSid = Sid.generate(Sid.Type.ACCOUNT);
        ProfileAssociation profileAssociation = new ProfileAssociation(profileSid, targetSid, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        //Add ProfileAssociation
        dao.addProfileAssociation(profileAssociation);
        
        //Read ProfileAssociation ByTargetSid
        ProfileAssociation resultantProfileAssociation = dao.getProfileAssociationByTargetSid(targetSid.toString());
        Assert.assertNotNull(resultantProfileAssociation);
        Assert.assertEquals(profileAssociation.toString(), resultantProfileAssociation.toString());

        //Read ProfileAssociation ByTargetSid
        List<ProfileAssociation> resultantProfileAssociations = dao.getProfileAssociationsByProfileSid(profileSid.toString());
        Assert.assertNotNull(resultantProfileAssociations);
        Assert.assertEquals(1, resultantProfileAssociations.size());
        Assert.assertEquals(profileAssociation.toString(), resultantProfileAssociations.get(0).toString());

        // Add another ProfileAssociation with same profile
        Sid targetSid2 = Sid.generate(Sid.Type.ACCOUNT);
        ProfileAssociation profileAssociation2 = new ProfileAssociation(profileSid, targetSid2, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        dao.addProfileAssociation(profileAssociation2);
        resultantProfileAssociations = dao.getProfileAssociationsByProfileSid(profileSid.toString());
        Assert.assertNotNull(resultantProfileAssociations);
        Assert.assertEquals(2, resultantProfileAssociations.size());

        //Update Associated Profile Of All Such ProfileSid
        //TODO: update profile sid type below after merge from master
        Sid newProfileSid = Sid.generate(Sid.Type.ORGANIZATION);
        dao.updateAssociatedProfileOfAllSuchProfileSid(profileSid.toString(), newProfileSid.toString());
        Assert.assertEquals(0, dao.getProfileAssociationsByProfileSid(profileSid.toString()).size());
        Assert.assertEquals(2, dao.getProfileAssociationsByProfileSid(newProfileSid.toString()).size());

        //Update ProfileAssociation of target
        //TODO: update profile sid type below after merge from master
        Sid newProfileSid2 = Sid.generate(Sid.Type.ORGANIZATION);
        profileAssociation = dao.getProfileAssociationsByProfileSid(newProfileSid.toString()).get(0);
        ProfileAssociation updatedProfileAssociation = profileAssociation.setProfileSid(newProfileSid2);
        dao.updateProfileAssociationOfTargetSid(updatedProfileAssociation);
        ProfileAssociation resultantProfileAssociationdao = dao.getProfileAssociationByTargetSid(updatedProfileAssociation.getTargetSid().toString());
        Assert.assertNotNull(resultantProfileAssociationdao);
        Assert.assertEquals(updatedProfileAssociation.getProfileSid().toString(), resultantProfileAssociationdao.getProfileSid().toString());

        //Delete ByProfileSid
        dao.deleteProfileAssociationByProfileSid(newProfileSid2.toString());
        Assert.assertEquals(0,dao.getProfileAssociationsByProfileSid(newProfileSid2.toString()).size());
        
        //Delete ByTargetSid
        dao.deleteProfileAssociationByTargetSid(resultantProfileAssociationdao.getProfileSid().toString());
        Assert.assertNull(dao.getProfileAssociationByTargetSid(resultantProfileAssociationdao.getProfileSid().toString()));
    }

}
