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

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readInteger;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readUri;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeUri;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readBlobData;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.QueueDao;
import org.mobicents.servlet.restcomm.entities.Queue;
import org.mobicents.servlet.restcomm.entities.QueueFilter;
import org.mobicents.servlet.restcomm.entities.QueueRecord;
import org.mobicents.servlet.restcomm.entities.Sid;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
public class MybatisQueueDao implements QueueDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.QueueDao.";
    private final SqlSessionFactory sessions;

    public MybatisQueueDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public Queue getQueue(Sid sid) {
        return getQueue(namespace + "getQueue", sid.toString());
    }

    @Override
    public List<Queue> getQueues(QueueFilter filter) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getQueues", filter);
            final List<Queue> queues = new ArrayList<Queue>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    queues.add(toQueue(result));
                }
            }
            return queues;
        } finally {
            session.close();
        }
    }

    @Override
    public void addQueue(Queue queue) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addQueue", toMap(queue));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeQueue(Sid sid) {
        removeQueue(namespace + "removeQueue", sid);

    }

    private void removeQueue(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateQueue(Queue queue) {
        updateQueue(namespace + "updateQueue", queue);
    }

    private void updateQueue(final String selector, final Queue queue) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(selector, toMap(queue));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Queue getQueueByFriendlyName(String friendlyName) {
        return getQueue(namespace + "getQueueByFriendlyName", friendlyName);
    }

    private Queue getQueue(final String selector, final Object parameters) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, parameters);
            if (result != null) {
                return toQueue(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public java.util.Queue<QueueRecord> getQueueBytes(Sid sid) {
        Queue queue = getQueue(namespace + "getQueue", sid.toString());
        return toCollectionFromBytes(queue.getQueue());
    }

    @Override
    public void setQueueBytes(java.util.Queue<QueueRecord> members, Queue queue) {
        byte[] binaryData = toBytesArray(members);
        queue = queue.setQueue(binaryData);
        updateQueue(namespace + "updateQueueBytes", queue);
        if (members.size() > 0) {
            queue = queue.setCurrentSize(members.size());
            updateQueue(namespace + "updateQueue", queue);
        }

    }

    @Override
    public int getTotalQueueByAccount(QueueFilter filter) {
        final SqlSession session = sessions.openSession();
        try {
            final Integer total = session.selectOne(namespace + "getTotalQueueByAccount", filter);
            return total;
        } finally {
            session.close();
        }

    }

    private Queue toQueue(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Integer currentSize = readInteger(map.get("current_size"));
        final Integer maxSize = readInteger(map.get("max_size"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final URI uri = readUri(map.get("uri"));
        final byte[] queue = readBlobData(map.get("queue"));

        return new Queue(sid, dateCreated, dateUpdated, friendlyName, currentSize, maxSize, accountSid, uri, queue);
    }

    private Map<String, Object> toMap(final Queue queue) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(queue.getSid()));
        map.put("date_created", writeDateTime(queue.getDateCreated()));
        map.put("date_updated", writeDateTime(queue.getDateUpdated()));
        map.put("friendly_name", queue.getFriendlyName());
        map.put("current_size", queue.getCurrentSize());
        map.put("max_size", queue.getMaxSize());
        map.put("account_sid", writeSid(queue.getAccountSid()));
        map.put("uri", writeUri(queue.getUri()));
        map.put("queue", queue.getQueue());
        return map;
    }

    private byte[] toBytesArray(java.util.Queue<QueueRecord> queue) {
        byte[] queueData = null;
        String jsonString = JsonWriter.objectToJson(queue);
        queueData = jsonString.getBytes();
        return queueData;
    }

    @SuppressWarnings("unchecked")
    private java.util.Queue<QueueRecord> toCollectionFromBytes(byte[] binaryData) {
        java.util.Queue<QueueRecord> queue = new java.util.LinkedList<QueueRecord>();
        queue = (java.util.Queue<QueueRecord>) JsonReader.jsonToJava(new String(binaryData));
        return queue;
    }

}
