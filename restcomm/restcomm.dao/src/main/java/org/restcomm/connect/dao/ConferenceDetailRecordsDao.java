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

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;
import org.restcomm.connect.dao.entities.ConferenceRecordCountFilter;

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

    Integer countByFilter(ConferenceRecordCountFilter filter);

    int addConferenceDetailRecord(ConferenceDetailRecord cdr);

    void removeConferenceDetailRecord(Sid sid);

    void removeConferenceDetailRecords(Sid accountSid);

    void updateConferenceDetailRecordStatus(ConferenceDetailRecord cdr);

    void updateConferenceDetailRecordMasterEndpointID(ConferenceDetailRecord cdr);

    void updateMasterPresent(ConferenceDetailRecord cdr);

    void updateConferenceDetailRecordMasterBridgeEndpointID(ConferenceDetailRecord cdr);

    void updateModeratorPresent(ConferenceDetailRecord cdr);

    /**
     * @param params
     * {sid, mode=IN, jdbcType=VARCHAR}
     * {status, mode=IN, jdbcType=VARCHAR}
     * {slaveMsId, mode=IN, jdbcType=VARCHAR}
     * {dateUpdated, mode=IN, jdbcType=TIMESTAMP}
     * {amIMaster, mode=IN, jdbcType=BOOLEAN}
     * {completed, mode=OUT, jdbcType=BOOLEAN}
     * @return true/false depending on if calling agent was able to complete the conference or not.
     */
    boolean completeConferenceDetailRecord(Map params);
}
