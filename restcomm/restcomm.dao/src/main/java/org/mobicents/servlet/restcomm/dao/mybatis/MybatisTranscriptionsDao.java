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

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Transcription;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisTranscriptionsDao implements TranscriptionsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao.";
    private final SqlSessionFactory sessions;

    public MybatisTranscriptionsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addTranscription(final Transcription transcription) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addTranscription", toMap(transcription));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Transcription getTranscription(final Sid sid) {
        return getTranscription(namespace + "getTranscription", sid);
    }

    @Override
    public Transcription getTranscriptionByRecording(final Sid recordingSid) {
        return getTranscription(namespace + "getTranscriptionByRecording", recordingSid);
    }

    private Transcription getTranscription(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, sid.toString());
            if (result != null) {
                return toTranscription(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Transcription> getTranscriptions(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session
                    .selectList(namespace + "getTranscriptions", accountSid.toString());
            final List<Transcription> transcriptions = new ArrayList<Transcription>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    transcriptions.add(toTranscription(result));
                }
            }
            return transcriptions;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeTranscription(final Sid sid) {
        removeTranscriptions(namespace + "removeTranscription", sid);
    }

    @Override
    public void removeTranscriptions(final Sid accountSid) {
        removeTranscriptions(namespace + "removeTranscriptions", accountSid);
    }

    private void removeTranscriptions(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateTranscription(final Transcription transcription) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateTranscription", toMap(transcription));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final Transcription transcription) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(transcription.getSid()));
        map.put("date_created", writeDateTime(transcription.getDateCreated()));
        map.put("date_updated", writeDateTime(transcription.getDateUpdated()));
        map.put("account_sid", writeSid(transcription.getAccountSid()));
        map.put("status", transcription.getStatus().toString());
        map.put("recording_sid", writeSid(transcription.getRecordingSid()));
        map.put("duration", transcription.getDuration());
        map.put("transcription_text", transcription.getTranscriptionText());
        map.put("price", writeBigDecimal(transcription.getPrice()));
        map.put("uri", writeUri(transcription.getUri()));
        return map;
    }

    private Transcription toTranscription(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final String text = readString(map.get("status"));
        final Transcription.Status status = Transcription.Status.getStatusValue(text);
        final Sid recordingSid = readSid(map.get("recording_sid"));
        final Double duration = readDouble(map.get("duration"));
        final String transcriptionText = readString(map.get("transcription_text"));
        final BigDecimal price = readBigDecimal(map.get("price"));
        final URI uri = readUri(map.get("uri"));
        return new Transcription(sid, dateCreated, dateUpdated, accountSid, status, recordingSid, duration, transcriptionText,
                price, uri);
    }
}
