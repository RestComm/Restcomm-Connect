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
package org.mobicents.servlet.restcomm.dao;

import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecord;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
public interface ConferenceDetailRecordsDao {
    ConferenceDetailRecord getConferenceDetailRecord(Sid sid);

    List<ConferenceDetailRecord> getConferenceDetailRecords(Sid accountSid);

    List<ConferenceDetailRecord> getConferenceDetailRecordsByStatus(String status);

    List<ConferenceDetailRecord> getConferenceDetailRecords(ConferenceDetailRecordFilter filter);

    List<ConferenceDetailRecord> getConferenceDetailRecordsByDateCreated(DateTime dateCreated);

    List<ConferenceDetailRecord> getConferenceDetailRecordsByDateUpdated(DateTime dateUpdated);

    Integer getTotalConferenceDetailRecords(ConferenceDetailRecordFilter filter);

    int addConferenceDetailRecord(ConferenceDetailRecord cdr);

    void removeConferenceDetailRecord(Sid sid);

    void removeConferenceDetailRecords(Sid accountSid);

    void updateConferenceDetailRecordStatus(ConferenceDetailRecord cdr);

    void updateConferenceDetailRecordMasterEndpointID(ConferenceDetailRecord cdr);

    void updateConferenceDetailRecordMasterIVREndpointID(ConferenceDetailRecord cdr);

    void updateMasterPresent(ConferenceDetailRecord cdr);
}
