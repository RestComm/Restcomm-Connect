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

package org.restcomm.connect.dao;

import org.apache.commons.configuration.Configuration;
import scala.concurrent.ExecutionContext;

/**
 * DaoManager mock class to be used for unit-testing endpoints. Add further Daos if needed.
 *
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class DaoManagerMock implements DaoManager {

    private AccountsDao accountsDao;

    public DaoManagerMock(AccountsDao accountsDao) {
        this.accountsDao = accountsDao;
    }

    @Override
    public AccountsDao getAccountsDao() {
        return accountsDao;
    }

    @Override
    public ApplicationsDao getApplicationsDao() {
        return null;
    }

    @Override
    public AnnouncementsDao getAnnouncementsDao() {
        return null;
    }

    @Override
    public AvailablePhoneNumbersDao getAvailablePhoneNumbersDao() {
        return null;
    }

    @Override
    public CallDetailRecordsDao getCallDetailRecordsDao() {
        return null;
    }

    @Override
    public ConferenceDetailRecordsDao getConferenceDetailRecordsDao() {
        return null;
    }

    @Override
    public ClientsDao getClientsDao() {
        return null;
    }

    @Override
    public HttpCookiesDao getHttpCookiesDao() {
        return null;
    }

    @Override
    public IncomingPhoneNumbersDao getIncomingPhoneNumbersDao() {
        return null;
    }

    @Override
    public NotificationsDao getNotificationsDao() {
        return null;
    }

    @Override
    public OutgoingCallerIdsDao getOutgoingCallerIdsDao() {
        return null;
    }

    @Override
    public RegistrationsDao getRegistrationsDao() {
        return null;
    }

    @Override
    public RecordingsDao getRecordingsDao() {
        return null;
    }

    @Override
    public ShortCodesDao getShortCodesDao() {
        return null;
    }

    @Override
    public SmsMessagesDao getSmsMessagesDao() {
        return null;
    }

    @Override
    public UsageDao getUsageDao() {
        return null;
    }

    @Override
    public TranscriptionsDao getTranscriptionsDao() {
        return null;
    }

    @Override
    public GatewaysDao getGatewaysDao() {
        return null;
    }

    @Override
    public InstanceIdDao getInstanceIdDao() {
        return null;
    }

    @Override
    public MediaServersDao getMediaServersDao() {
        return null;
    }

    @Override
    public MediaResourceBrokerDao getMediaResourceBrokerDao() {
        return null;
    }

    @Override
    public ExtensionsConfigurationDao getExtensionsConfigurationDao() {
        return null;
    }

    @Override
    public GeolocationDao getGeolocationDao() {
        return null;
    }

    @Override
    public void configure(Configuration configuration, Configuration daoManagerConfiguration, ExecutionContext ec) {

    }

    @Override
    public void start() throws RuntimeException {

    }

    @Override
    public void shutdown() throws InterruptedException {

    }
}
