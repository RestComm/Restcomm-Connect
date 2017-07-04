package org.restcomm.connect.telephony.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.mybatis.MybatisDaoManager;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;

import akka.actor.ActorSystem;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class ConferenceTestUtil {
	private final static Logger logger = Logger.getLogger(ConferenceTestUtil.class.getName());

	protected static ActorSystem system;

    protected static Configuration configurationNode1;
    protected static Configuration configurationNode2;
    protected XMLConfiguration daoManagerConf = null;

    protected static MybatisDaoManager daoManager;
    protected static final String CONFIG_PATH_NODE_1 = "/restcomm.xml";
    protected static final String CONFIG_PATH_NODE_2 = "/restcomm-node2.xml";
    protected static final String CONFIG_PATH_DAO_MANAGER = "/dao-manager.xml";

    protected static final String CONFERENCE_FRIENDLY_NAME_1 ="ACae6e420f425248d6a26948c17a9e2acf:1111";
    protected static final String CONFERENCE_FRIENDLY_NAME_2 ="ACae6e420f425248d6a26948c17a9e2acf:1122";

    protected static final String CALL_SID ="CAae6e420f425248d6a26948c17a9e2acf";

    protected static final String ACCOUNT_SID_1 ="ACae6e420f425248d6a26948c17a9e2acf";

    @BeforeClass
    public static void beforeClass() throws Exception {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void afterClass() throws Exception {
    	system.shutdown();
    }

    protected Configuration createCfg(final String cfgFileName) throws ConfigurationException, MalformedURLException {
		URL url = this.getClass().getResource(cfgFileName);
		return new XMLConfiguration(url);
	}

    protected Configuration createDaoManagerCfg(final String cfgFileName) throws ConfigurationException, MalformedURLException {
    	URL url = this.getClass().getResource(cfgFileName);
		return new XMLConfiguration(url);
	}

    private void cleanAllConferences() {
    	ConferenceDetailRecordsDao dao = daoManager.getConferenceDetailRecordsDao();
    	List<ConferenceDetailRecord> records = (List) dao.getConferenceDetailRecords(new Sid(ACCOUNT_SID_1));
		for(int i=0; i<records.size(); i++){
			ConferenceDetailRecord cdr = records.get(i);
			cdr = cdr.setStatus(ConferenceStateChanged.State.COMPLETED+"");
			dao.updateConferenceDetailRecordStatus(cdr);
		}
	}

	protected void startDaoManager() throws ConfigurationException, MalformedURLException{
        daoManagerConf = (XMLConfiguration)createDaoManagerCfg(CONFIG_PATH_DAO_MANAGER);
        daoManager = new MybatisDaoManager();
        daoManager.configure(configurationNode1, daoManagerConf );
        daoManager.start();
	}

}
