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
import org.mobicents.servlet.restcomm.dao.UsageDao;
import org.mobicents.servlet.restcomm.amazonS3.S3AccessTool;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisDaoManager implements DaoManager {
    private Configuration configuration;
    private Configuration amazonS3Configuration;
    private Configuration runtimeConfiguration;
    private S3AccessTool s3AccessTool;
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
    private UsageDao usageDao;
    private TranscriptionsDao transcriptionsDao;
    private GatewaysDao gatewaysDao;
    private AnnouncementsDao announcementsDao;

    public MybatisDaoManager() {
        super();
    }

    @Override
    public void configure(final Configuration configuration) {
        this.configuration = configuration.subset("dao-manager");
        this.amazonS3Configuration = configuration.subset("amazon-s3");
        this.runtimeConfiguration = configuration.subset("runtime-settings");
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
    public UsageDao getUsageDao() {
        return usageDao;
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
        boolean amazonS3Enabled = amazonS3Configuration.getBoolean("enabled");
        if (amazonS3Enabled) {
            final String accessKey = amazonS3Configuration.getString("access-key");
            final String securityKey = amazonS3Configuration.getString("security-key");
            final String bucketName = amazonS3Configuration.getString("bucket-name");
            final String folder = amazonS3Configuration.getString("folder");
            final boolean reducedRedundancy = amazonS3Configuration.getBoolean("reduced-redundancy");
            final int daysToRetainPublicUrl = amazonS3Configuration.getInt("days-to-retain-public-url");
            final boolean removeOriginalFile = amazonS3Configuration.getBoolean("remove-original-file");
            s3AccessTool = new S3AccessTool(accessKey, securityKey, bucketName, folder, reducedRedundancy, daysToRetainPublicUrl, removeOriginalFile);
        }
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
        if (s3AccessTool != null) {
            final String recordingPath = runtimeConfiguration.getString("recordings-path");
            recordingsDao = new MybatisRecordingsDao(sessions, s3AccessTool, recordingPath);
        } else {
            recordingsDao = new MybatisRecordingsDao(sessions);
        }
        shortCodesDao = new MybatisShortCodesDao(sessions);
        smsMessagesDao = new MybatisSmsMessagesDao(sessions);
        usageDao = new MybatisUsageDao(sessions);
        transcriptionsDao = new MybatisTranscriptionsDao(sessions);
        gatewaysDao = new MybatisGatewaysDao(sessions);
    }
}
