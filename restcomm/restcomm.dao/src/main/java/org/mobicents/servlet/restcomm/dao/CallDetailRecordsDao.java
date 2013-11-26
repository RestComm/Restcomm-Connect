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
package org.mobicents.servlet.restcomm.dao;

import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface CallDetailRecordsDao {
    void addCallDetailRecord(CallDetailRecord cdr);

    CallDetailRecord getCallDetailRecord(Sid sid);

    List<CallDetailRecord> getCallDetailRecords(Sid accountSid);

    List<CallDetailRecord> getCallDetailRecordsByRecipient(String recipient);

    List<CallDetailRecord> getCallDetailRecordsBySender(String sender);

    List<CallDetailRecord> getCallDetailRecordsByStatus(String status);

    List<CallDetailRecord> getCallDetailRecordsByStartTime(DateTime startTime);

    List<CallDetailRecord> getCallDetailRecordsByParentCall(Sid parentCallSid);

    void removeCallDetailRecord(Sid sid);

    void removeCallDetailRecords(Sid accountSid);

    void updateCallDetailRecord(CallDetailRecord cdr);

    // Support for filtering of calls list result, Issue 153
    List<CallDetailRecord> getCallDetailRecords(CallDetailRecordFilter filter);

    Integer getTotalCallDetailRecords(CallDetailRecordFilter filter);
}
