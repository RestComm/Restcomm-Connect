package org.restcomm.connect.dao.mybatis;

import java.io.File;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.restcomm.connect.commons.annotations.UnstableTests;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionRules;

import java.io.InputStream;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Created by gvagenas on 21/10/2016.
 */
@Category(UnstableTests.class)
@Ignore
public class ExtensionRulesDaoTest {
    private static MybatisDaoManager manager;
    private MybatisExtensionsRulesDao extensionsRulesDao;

    @Rule
    public TestName name = new TestName();

    private String validJsonObject = "{\n" +
            "  \"project\": \"Restcomm-Connect\",\n" +
            "  \"url\": \"http://restcomm.com\",\n" +
            "  \"open-source\": true,\n" +
            "  \"scm\": {\n" +
            "    \"url\": \"https://github.com/RestComm/Restcomm-Connect/\",\n" +
            "    \"issues\": \"https://github.com/RestComm/Restcomm-Connect/issues\"\n" +
            "  },\n" +
            "  \"ci\": {\n" +
            "      \"name\": \"Jenkins\",\n" +
            "      \"url\": \"https://mobicents.ci.cloudbees.com/view/RestComm/job/RestComm/\"\n" +
            "  },\n" +
            "  \"active\": \"true\"\n" +
            "}";

    private String invalidJsonObject = "{\n" +
            "  project: Restcomm-Connect,\n" +
            "  \"url\": \"http://restcomm.com\",\n" +
            "  \"open-source\": true,\n" +
            "  \"scm\": {\n" +
            "    \"url\": \"https://github.com/RestComm/Restcomm-Connect/\",\n" +
            "    \"issues\": \"https://github.com/RestComm/Restcomm-Connect/issues\"\n" +
            "  }\n" +
            "  \"ci\": [\n" +
            "    {\n" +
            "      \"name\": \"Jenkins\",\n" +
            "      \"url\": \"https://mobicents.ci.cloudbees.com/view/RestComm/job/RestComm/\"\n" +
            "    }\n" +
            "  \"active\": \"true\"\n" +
            "}";

    private String validXmlDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "  <name>Restcomm-Connect</name>\n" +
            "  <url>http://restcomm.com</url>\n" +
            "  <open-source>true</open-source>\n" +
            "  <scm>\n" +
            "    <url>https://github.com/RestComm/Restcomm-Connect/</url>\n" +
            "    <issues>https://github.com/RestComm/Restcomm-Connect/issues</issues>\n" +
            "  </scm>\n" +
            "  <ci>\n" +
            "    <name>Jenkins</name>\n" +
            "    <url>https://mobicents.ci.cloudbees.com/view/RestComm/job/RestComm/</url>\n" +
            "  </ci>\n" +
            "</project>";

    private String invalidXmlDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "  <name>Restcomm-Connect</name>\n" +
            "  <url>http://restcomm.com</url>\n" +
            "  <open-source>true</open-source>\n" +
            "  <scm>\n" +
            "    <url>https://github.com/RestComm/Restcomm-Connect/</url>\n" +
            "    <issues>https://github.com/RestComm/Restcomm-Connect/issues</issues>\n" +
            "  </scm>\n" +
            "  <ci>\n" +
            "    <name>Jenkins</name>\n" +
            "    <url>https://mobicents.ci.cloudbees.com/view/RestComm/job/RestComm/</url>\n" +
            "  </ci>";

    private String updatedJsonObject = "{\n" +
            "  \"project\": \"Restcomm-Connect\",\n" +
            "  \"url\": \"http://restcomm.com\",\n" +
            "  \"open-source\": true,\n" +
            "  \"scm\": {\n" +
            "    \"url\": \"https://github.com/RestComm/Restcomm-Connect/\",\n" +
            "    \"issues\": \"https://github.com/RestComm/Restcomm-Connect/issues\"\n" +
            "  },\n" +
            "  \"ci\": {\n" +
            "      \"name\": \"Jenkins\",\n" +
            "      \"url\": \"https://mobicents.ci.cloudbees.com/view/RestComm/job/RestComm/\"\n" +
            "  },\n" +
            "  \"active\": \"false\"\n" +
            "}";

    private String updatedXmlDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "  <name>Restcomm-Connect</name>\n" +
            "  <url>http://restcomm.com</url>\n" +
            "  <open-source>true</open-source>\n" +
            "  <scm>\n" +
            "    <url>https://github.com/RestComm/Restcomm-Connect/</url>\n" +
            "    <issues>https://github.com/RestComm/Restcomm-Connect/issues</issues>\n" +
            "  </scm>\n" +
            "  <ci>\n" +
            "    <name>Jenkins</name>\n" +
            "    <url>https://mobicents.ci.cloudbees.com/view/RestComm/job/RestComm/</url>\n" +
            "  </ci>\n" +
            "  <active>true</active>\n" +
            "</project>";

    @Before
    public void before() throws Exception{
        //use a different data dir for each test case to provide isolation
        Properties properties = new Properties();
        File srcFile = new File("./target/test-classes/data/restcomm.script");
        File theDir = new File("./target/test-classes/data" + name.getMethodName());
        theDir.mkdir();
        File destFile = new File("./target/test-classes/data" + name.getMethodName() + "/restcomm.script");

        Files.copy(srcFile.toPath(),
                destFile.toPath(), REPLACE_EXISTING);
        properties.setProperty("data", name.getMethodName());

        final InputStream data = getClass().getResourceAsStream("/mybatis_pertest.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data,properties);
        manager = new MybatisDaoManager();
        manager.start(factory);
        extensionsRulesDao = (MybatisExtensionsRulesDao) manager.getExtensionsRulesDao();
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void testValidJsonDoc() {
        ExtensionRules validExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                "testExt", true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        assertEquals(true, extensionsRulesDao.validate(validExtensionRules));
    }

    @Test
    public void testInvalidJsonDoc() {
        ExtensionRules invalidExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                "testExt", true, invalidJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        assertEquals(false, extensionsRulesDao.validate(invalidExtensionRules));
    }

    @Test
    public void testValidXmlDoc() {
        ExtensionRules validExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                "testExt", true, validXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        assertEquals(true, extensionsRulesDao.validate(validExtensionRules));
    }

    @Test
    public void testInvalidXmlDoc() {
        ExtensionRules invalidExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                "testExt", true, invalidXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        assertEquals(false, extensionsRulesDao.validate(invalidExtensionRules));
    }

    @Test
    public void testStoreAndRetrieveConfigurationByNameJson() throws ConfigurationException {
        String extName = "testExt";
        ExtensionRules validExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                extName, true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        extensionsRulesDao.deleteExtensionRulesByName(extName);
    }

    @Test
    public void testStoreAndRetrieveConfigurationBySidJson() throws ConfigurationException {
        Sid sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        ExtensionRules validExtensionRules = new ExtensionRules(sid,
                "testExt", true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        extensionsRulesDao.deleteExtensionRulesBySid(sid);
    }

    @Test
    public void testStoreUpdateAndRetrieveConfigurationByNameJson() throws ConfigurationException {
        String extName = "testExt";
        ExtensionRules validExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                extName, true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        validExtensionRules.setConfigurationData(updatedJsonObject, ExtensionRules.configurationType.JSON);
        extensionsRulesDao.updateExtensionRules(validExtensionRules);
        retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertEquals(updatedJsonObject, retrievedConf.getConfigurationData());
        extensionsRulesDao.deleteExtensionRulesByName(extName);
    }

    @Test
    public void testStoreUpdateAndRetrieveConfigurationBySidJson() throws ConfigurationException {
        Sid sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        ExtensionRules validExtensionRules = new ExtensionRules(sid,
                "testExt", true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        validExtensionRules.setConfigurationData(updatedJsonObject, ExtensionRules.configurationType.JSON);
        extensionsRulesDao.updateExtensionRules(validExtensionRules);
        retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertEquals(updatedJsonObject, retrievedConf.getConfigurationData());
        extensionsRulesDao.deleteExtensionRulesBySid(sid);
    }

    @Test
    public void testStoreAndRetrieveConfigurationByNameXml() throws ConfigurationException {
        String extName = "testExt";
        ExtensionRules validExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                extName, true, validXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        extensionsRulesDao.deleteExtensionRulesByName(extName);
    }

    @Test
    public void testStoreAndRetrieveConfigurationBySidXml() throws ConfigurationException {
        Sid sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        ExtensionRules validExtensionRules = new ExtensionRules(sid,
                "testExt", true, validXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        extensionsRulesDao.deleteExtensionRulesBySid(sid);
    }

    @Test
    public void testStoreUpdateAndRetrieveConfigurationByNameXml() throws ConfigurationException {
        String extName = "testExt";
        ExtensionRules validExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                extName, true, validXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        validExtensionRules.setConfigurationData(updatedXmlDoc, ExtensionRules.configurationType.XML);
        extensionsRulesDao.updateExtensionRules(validExtensionRules);
        retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertEquals(updatedXmlDoc, retrievedConf.getConfigurationData());
        extensionsRulesDao.deleteExtensionRulesByName(extName);
    }

    @Test
    public void testStoreUpdateAndRetrieveConfigurationBySidXml() throws ConfigurationException {
        Sid sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        ExtensionRules validExtensionRules = new ExtensionRules(sid,
                "testExt", true, validXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        extensionsRulesDao.addExtensionRules(validExtensionRules);
        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), validExtensionRules.getConfigurationData().toString());
        validExtensionRules.setConfigurationData(updatedXmlDoc, ExtensionRules.configurationType.XML);
        extensionsRulesDao.updateExtensionRules(validExtensionRules);
        retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertEquals(updatedXmlDoc, retrievedConf.getConfigurationData());
        extensionsRulesDao.deleteExtensionRulesBySid(sid);
    }

    @Test
    public void testStoreAndGetAllConfiguration() throws ConfigurationException {
        Sid sid1 = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        Sid sid2 = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        ExtensionRules validExtensionRulesXml = new ExtensionRules(sid1,
                "testExtXml", true, validXmlDoc, ExtensionRules.configurationType.XML, DateTime.now());
        ExtensionRules validExtensionRulesJson = new ExtensionRules(sid2,
                "testExtJson", true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());

        extensionsRulesDao.addExtensionRules(validExtensionRulesXml);
        extensionsRulesDao.addExtensionRules(validExtensionRulesJson);

        List<ExtensionRules> confs = extensionsRulesDao.getAllExtensionRules();

        assertEquals(2, confs.size());

        extensionsRulesDao.deleteExtensionRulesBySid(sid1);
        extensionsRulesDao.deleteExtensionRulesBySid(sid2);

        confs = extensionsRulesDao.getAllExtensionRules();
        assertEquals(0, confs.size());
    }

    @Test
    public void testStoreUpdateAndRetrieveConfigurationByNameCheckVersionJson() throws ConfigurationException {
        String extName = "testExt";
        ExtensionRules originalExtensionRules = new ExtensionRules(Sid.generate(Sid.Type.EXTENSION_CONFIGURATION),
                extName, true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        extensionsRulesDao.addExtensionRules(originalExtensionRules);

        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesByName(extName);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), originalExtensionRules.getConfigurationData().toString());

        DateTime originalDateUpdated = retrievedConf.getDateUpdated();

        boolean isUpdated = extensionsRulesDao.isLatestVersionByName(extName, originalDateUpdated);
        assertEquals(isUpdated , false);

        originalExtensionRules.setConfigurationData(updatedJsonObject, ExtensionRules.configurationType.JSON);
        extensionsRulesDao.updateExtensionRules(originalExtensionRules);

        isUpdated = extensionsRulesDao.isLatestVersionByName(extName, originalDateUpdated);
        assertEquals(isUpdated , true);

        extensionsRulesDao.deleteExtensionRulesByName(extName);
    }

    @Test
    public void testStoreUpdateAndRetrieveConfigurationBySidCheckVersionJson() throws ConfigurationException {
        String extName = "testExt";
        Sid sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        ExtensionRules originalExtensionRules = new ExtensionRules(sid,
                extName, true, validJsonObject, ExtensionRules.configurationType.JSON, DateTime.now());
        extensionsRulesDao.addExtensionRules(originalExtensionRules);

        ExtensionRules retrievedConf = extensionsRulesDao.getExtensionRulesBySid(sid);
        assertNotNull(retrievedConf);
        assertEquals(retrievedConf.getConfigurationData().toString(), originalExtensionRules.getConfigurationData().toString());

        DateTime originalDateUpdated = retrievedConf.getDateUpdated();

        boolean isUpdated = extensionsRulesDao.isLatestVersionBySid(sid, originalDateUpdated);
        assertEquals(isUpdated , false);

        originalExtensionRules.setConfigurationData(updatedJsonObject, ExtensionRules.configurationType.JSON);
        extensionsRulesDao.updateExtensionRules(originalExtensionRules);

        isUpdated = extensionsRulesDao.isLatestVersionBySid(sid, originalDateUpdated);
        assertEquals(isUpdated , true);

        extensionsRulesDao.deleteExtensionRulesBySid(sid);
    }
}
