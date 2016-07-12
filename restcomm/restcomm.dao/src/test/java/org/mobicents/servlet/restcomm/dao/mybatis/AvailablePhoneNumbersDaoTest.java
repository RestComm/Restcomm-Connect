package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.AvailablePhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumber;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public class AvailablePhoneNumbersDaoTest {

	private static MybatisDaoManager manager;

    public AvailablePhoneNumbersDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
    }
    
    
    @Test
    public void createReadUpdateDelete(){
    	String friendlyName="testAccount";
        String phoneNumber="+112666666666";
        Integer lata=1;
        String rateCenter="center1";
        Double latitude=32.12234;
        Double longitude=-6.34445;
        String region="MA";
        Integer postalCode=10000;
        String isoCountry="US";
        String cost="testCotst";

        // Capabilities
        Boolean voiceCapable=true;
        Boolean smsCapable=true;
        Boolean mmsCapable=true;
        Boolean faxCapable=true;
        
        AvailablePhoneNumber apn= new AvailablePhoneNumber(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude, region, postalCode, isoCountry, cost, voiceCapable, smsCapable, mmsCapable, faxCapable);
        
        final AvailablePhoneNumbersDao dao=manager.getAvailablePhoneNumbersDao();
        
        dao.addAvailablePhoneNumber(apn);

        List<AvailablePhoneNumber> list=dao.getAvailablePhoneNumbers();
        assertEquals(1,list.size());
        
        apn=list.get(0);
        assertEquals(friendlyName,apn.getFriendlyName());
        assertEquals(phoneNumber,apn.getPhoneNumber());
        assertEquals(lata,apn.getLata());
        assertEquals(latitude,apn.getLatitude());
        assertEquals(longitude,apn.getLongitude());
        assertEquals(region,apn.getRegion());
        assertEquals(cost,apn.getCost());
        assertEquals(voiceCapable,apn.isVoiceCapable());
        assertEquals(mmsCapable,apn.isMmsCapable());

        list=dao.getAvailablePhoneNumbersByAreaCode("12");
        assertEquals(1,list.size());
        
        apn=list.get(0);
        assertEquals(friendlyName,apn.getFriendlyName());
        assertEquals(phoneNumber,apn.getPhoneNumber());
        assertEquals(lata,apn.getLata());
        assertEquals(latitude,apn.getLatitude());
        assertEquals(longitude,apn.getLongitude());
        assertEquals(region,apn.getRegion());
        assertEquals(cost,apn.getCost());
        assertEquals(voiceCapable,apn.isVoiceCapable());
        assertEquals(mmsCapable,apn.isMmsCapable());

        list=dao.getAvailablePhoneNumbersByPattern("+1*******6666");
        assertEquals(1,list.size());
        
        apn=list.get(0);
        assertEquals(friendlyName,apn.getFriendlyName());
        assertEquals(phoneNumber,apn.getPhoneNumber());
        assertEquals(lata,apn.getLata());
        assertEquals(latitude,apn.getLatitude());
        assertEquals(longitude,apn.getLongitude());
        assertEquals(region,apn.getRegion());
        assertEquals(cost,apn.getCost());
        assertEquals(voiceCapable,apn.isVoiceCapable());
        assertEquals(mmsCapable,apn.isMmsCapable());

        list=dao.getAvailablePhoneNumbersByRegion("MA");
        assertEquals(1,list.size());
        
        apn=list.get(0);
        assertEquals(friendlyName,apn.getFriendlyName());
        assertEquals(phoneNumber,apn.getPhoneNumber());
        assertEquals(lata,apn.getLata());
        assertEquals(latitude,apn.getLatitude());
        assertEquals(longitude,apn.getLongitude());
        assertEquals(region,apn.getRegion());
        assertEquals(cost,apn.getCost());
        assertEquals(voiceCapable,apn.isVoiceCapable());
        assertEquals(mmsCapable,apn.isMmsCapable());

        list=dao.getAvailablePhoneNumbersByPostalCode(10000);
        assertEquals(1,list.size());
        
        apn=list.get(0);
        assertEquals(friendlyName,apn.getFriendlyName());
        assertEquals(phoneNumber,apn.getPhoneNumber());
        assertEquals(lata,apn.getLata());
        assertEquals(latitude,apn.getLatitude());
        assertEquals(longitude,apn.getLongitude());
        assertEquals(region,apn.getRegion());
        assertEquals(cost,apn.getCost());
        assertEquals(voiceCapable,apn.isVoiceCapable());
        assertEquals(mmsCapable,apn.isMmsCapable());

    }
}
