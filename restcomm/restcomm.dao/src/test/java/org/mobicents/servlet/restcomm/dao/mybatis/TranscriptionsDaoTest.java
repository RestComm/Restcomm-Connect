package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Transcription;

public class TranscriptionsDaoTest {
    private static MybatisDaoManager manager;

    public TranscriptionsDaoTest() {
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
    public void createReadUpdateDelete() {
        final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid recordingSid = Sid.generate(Sid.Type.RECORDING);
        final URI url = URI.create("2012-04-24/Accounts/Acoount/SMS/Messages/unique-id.json");
        final Transcription.Builder builder = Transcription.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setStatus(Transcription.Status.IN_PROGRESS);
        builder.setRecordingSid(recordingSid);
        builder.setDuration(129.5);
        builder.setTranscriptionText("Hello World!");
        builder.setPrice(new BigDecimal(12));
        builder.setPriceUnit(Currency.getInstance("USD"));
        builder.setUri(url);
        Transcription transcription=builder.build();
        TranscriptionsDao dao=manager.getTranscriptionsDao();
        dao.addTranscription(transcription);
        
        transcription=dao.getTranscription(sid);
        assertEquals(account.toString(),transcription.getAccountSid().toString());
        assertEquals(url.toString(), transcription.getUri().toString());
        assertEquals("Hello World!",transcription.getTranscriptionText());

        transcription=dao.getTranscriptionByRecording(recordingSid);
        assertEquals(account.toString(),transcription.getAccountSid().toString());
        assertEquals(url.toString(), transcription.getUri().toString());
        assertEquals("Hello World!",transcription.getTranscriptionText());
        
        Transcription.Builder b = Transcription.builder();
        b.setSid(sid);
        b.setStatus(Transcription.Status.COMPLETED);
        b.setTranscriptionText("Hello World! 2");
        transcription=b.build();
        dao.updateTranscription(transcription);
        transcription=dao.getTranscription(sid);
        assertEquals(account.toString(),transcription.getAccountSid().toString());
        assertEquals(Transcription.Status.COMPLETED.toString(), transcription.getStatus().toString());
        assertEquals("Hello World! 2",transcription.getTranscriptionText());
        
        dao.removeTranscription(sid);
        transcription=dao.getTranscription(sid);
        assertNull(transcription);
    }
}
