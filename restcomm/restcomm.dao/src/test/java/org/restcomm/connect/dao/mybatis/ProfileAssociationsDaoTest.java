package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.ProfileAssociationsDao;
import org.restcomm.connect.dao.entities.ProfileAssociation;

import static org.junit.Assert.assertEquals;
import org.restcomm.connect.dao.ProfilesDao;
import org.restcomm.connect.dao.entities.Profile;

public class ProfileAssociationsDaoTest extends DaoTest {

    private static MybatisDaoManager manager;
    private static final String jsonProfile = "{}";

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
        Assert.assertEquals(profileAssociation.getProfileSid(), resultantProfileAssociation.getProfileSid());

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

        //Delete ByTargetSid
        int removed = dao.deleteProfileAssociationByTargetSid(resultantProfileAssociationdao.getTargetSid().toString(),
                resultantProfileAssociationdao.getProfileSid().toString());
        assertEquals(1, removed);
        ProfileAssociation profileAssociationByTargetSid = dao.getProfileAssociationByTargetSid(resultantProfileAssociationdao.getTargetSid().toString());
        Assert.assertNull(profileAssociationByTargetSid);

        //Delete ByProfileSid
        dao.deleteProfileAssociationByProfileSid(newProfileSid2.toString());
        Assert.assertEquals(0, dao.getProfileAssociationsByProfileSid(newProfileSid2.toString()).size());

    }


    /**
     * changep rofile for an account.
     *
     * Simlaute disorder of unlink7link in network
     * @throws IllegalArgumentException
     * @throws URISyntaxException
     */
    @Test
    public void changeProfile() throws IllegalArgumentException, URISyntaxException {
        ProfileAssociationsDao dao = manager.getProfileAssociationsDao();
        ProfilesDao profileDao = manager.getProfilesDao();

        Sid targetSid = Sid.generate(Sid.Type.ACCOUNT);
        Profile profile = new Profile(Sid.generate(Sid.Type.PROFILE).toString(), jsonProfile, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        Profile profile2 = new Profile(Sid.generate(Sid.Type.PROFILE).toString(), jsonProfile, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        profileDao.addProfile(profile);
        profileDao.addProfile(profile2);
        ProfileAssociation profileAssociation = new ProfileAssociation(new Sid(profile.getSid()), targetSid, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        dao.addProfileAssociation(profileAssociation);


        //linking to new profile comes before unlinking to previous
        ProfileAssociation profileAssociation2 = new ProfileAssociation(new Sid(profile2.getSid()), targetSid, Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        dao.deleteProfileAssociationByTargetSid(targetSid.toString());
        dao.addProfileAssociation(profileAssociation2);

        //unlinking to previous comes after
        int removed = dao.deleteProfileAssociationByTargetSid(targetSid.toString(), profile.getSid());
        //the association was removed in last linking
        assertEquals(0, removed);
    }
}
