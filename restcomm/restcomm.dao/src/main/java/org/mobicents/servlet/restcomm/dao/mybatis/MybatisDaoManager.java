/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.AnnouncementsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.AvailablePhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.restcomm.dao.HttpCookiesDao;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.dao.OutgoingCallerIdsDao;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.dao.ShortCodesDao;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisDaoManager implements DaoManager {
    private Configuration configuration;
    private AccountsDao accountsDao;
    private ApplicationsDao applicationsDao;
    private AvailablePhoneNumbersDao availablePhoneNumbersDao;
    private CallDetailRecordsDao callDetailRecordsDao;
    private ClientsDao clientsDao;
    private HttpCookiesDao httpCookiesDao;
    private IncomingPhoneNumbersDao incomingPhoneNumbersDao;
    private NotificationsDao notificationsDao;
    private OutgoingCallerIdsDao outgoingCallerIdsDao;
    private RegistrationsDao presenceRecordsDao;
    private RecordingsDao recordingsDao;
    private ShortCodesDao shortCodesDao;
    private SmsMessagesDao smsMessagesDao;
    private TranscriptionsDao transcriptionsDao;
    private GatewaysDao gatewaysDao;
    private AnnouncementsDao announcementsDao;

    public MybatisDaoManager() {
        super();
    }

    @Override
    public void configure(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public AccountsDao getAccountsDao() {
        return accountsDao;
    }

    @Override
    public ApplicationsDao getApplicationsDao() {
        return applicationsDao;
    }

    @Override
    public AnnouncementsDao getAnnouncementsDao() {
        return announcementsDao;
    }

    @Override
    public AvailablePhoneNumbersDao getAvailablePhoneNumbersDao() {
        return availablePhoneNumbersDao;
    }

    @Override
    public CallDetailRecordsDao getCallDetailRecordsDao() {
        return callDetailRecordsDao;
    }

    @Override
    public ClientsDao getClientsDao() {
        return clientsDao;
    }

    @Override
    public HttpCookiesDao getHttpCookiesDao() {
        return httpCookiesDao;
    }

    @Override
    public IncomingPhoneNumbersDao getIncomingPhoneNumbersDao() {
        return incomingPhoneNumbersDao;
    }

    @Override
    public NotificationsDao getNotificationsDao() {
        return notificationsDao;
    }

    @Override
    public RegistrationsDao getRegistrationsDao() {
        return presenceRecordsDao;
    }

    @Override
    public OutgoingCallerIdsDao getOutgoingCallerIdsDao() {
        return outgoingCallerIdsDao;
    }

    @Override
    public RecordingsDao getRecordingsDao() {
        return recordingsDao;
    }

    @Override
    public ShortCodesDao getShortCodesDao() {
        return shortCodesDao;
    }

    @Override
    public SmsMessagesDao getSmsMessagesDao() {
        return smsMessagesDao;
    }

    @Override
    public TranscriptionsDao getTranscriptionsDao() {
        return transcriptionsDao;
    }

    @Override
    public GatewaysDao getGatewaysDao() {
        return gatewaysDao;
    }

    @Override
    public void shutdown() {
        // Nothing to do.
    }

    @Override
    public void start() throws RuntimeException {
        // This must be called before any other MyBatis methods.
        org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
        // Load the configuration file.
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final String path = configuration.getString("configuration-file");
        Reader reader = null;
        try {
            reader = new FileReader(path);
        } catch (final FileNotFoundException exception) {
            throw new RuntimeException(exception);
        }
        final Properties properties = new Properties();
        final String dataFiles = configuration.getString("data-files");
        final String sqlFiles = configuration.getString("sql-files");
        properties.setProperty("data", dataFiles);
        properties.setProperty("sql", sqlFiles);
        final SqlSessionFactory sessions = builder.build(reader, properties);
        start(sessions);
    }

    public void start(final SqlSessionFactory sessions) {
        // Instantiate the DAO objects.
        accountsDao = new MybatisAccountsDao(sessions);
        applicationsDao = new MybatisApplicationsDao(sessions);
        announcementsDao = new MybatisAnnouncementsDao(sessions);
        availablePhoneNumbersDao = new MybatisAvailablePhoneNumbersDao(sessions);
        callDetailRecordsDao = new MybatisCallDetailRecordsDao(sessions);
        clientsDao = new MybatisClientsDao(sessions);
        httpCookiesDao = new MybatisHttpCookiesDao(sessions);
        incomingPhoneNumbersDao = new MybatisIncomingPhoneNumbersDao(sessions);
        notificationsDao = new MybatisNotificationsDao(sessions);
        outgoingCallerIdsDao = new MybatisOutgoingCallerIdsDao(sessions);
        presenceRecordsDao = new MybatisRegistrationsDao(sessions);
        recordingsDao = new MybatisRecordingsDao(sessions);
        shortCodesDao = new MybatisShortCodesDao(sessions);
        smsMessagesDao = new MybatisSmsMessagesDao(sessions);
        transcriptionsDao = new MybatisTranscriptionsDao(sessions);
        gatewaysDao = new MybatisGatewaysDao(sessions);
    }
}
