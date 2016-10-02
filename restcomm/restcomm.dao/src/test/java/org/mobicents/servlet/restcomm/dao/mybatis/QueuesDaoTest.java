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

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.LinkedList;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.QueuesDao;
import org.mobicents.servlet.restcomm.entities.Queue;
import org.mobicents.servlet.restcomm.entities.QueueFilter;
import org.mobicents.servlet.restcomm.entities.QueueRecord;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
public final class QueuesDaoTest {

    private static MybatisDaoManager manager;

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
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.QUEUE);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");

        final Queue.Builder builder = Queue.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Queue");
        builder.setMaxSize(500);
        builder.setAccountSid(account);
        builder.setUri(url);

        Queue queue = builder.build();
        final QueuesDao queueDao = manager.getQueuesDao();

        // Create a new queue in the data store.
        queueDao.addQueue(queue);

        // Read the queue from the data store.
        Queue result = queueDao.getQueue(sid);

        // Validate the results.
        assertTrue(result.getSid().equals(queue.getSid()));
        assertTrue(result.getFriendlyName().equals(queue.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(queue.getAccountSid()));
        assertTrue(result.getMaxSize().equals(queue.getMaxSize()));

        // Update the queue.
        url = URI.create("http://127.0.0.1:8080/restcomm/demos/world-hello.xml");
        queue = queue.setFriendlyName("Test Queue update");
        queue = queue.setMaxSize(600);
        queueDao.updateQueue(queue);

        // Read the updated queue from the data store.
        result = queueDao.getQueue(sid);

        // Validate the results.
        assertTrue(result.getSid().equals(queue.getSid()));
        assertTrue(result.getFriendlyName().equals(queue.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(queue.getAccountSid()));
        assertTrue(result.getMaxSize().equals(queue.getMaxSize()));

        // Delete the queue.
        queueDao.removeQueue(sid);

        // Validate that the queue was removed.
        assertTrue(queueDao.getQueue(sid) == null);
    }

    @Test
    public void removeByAccountSid() {
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.QUEUE);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final Queue.Builder builder = Queue.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Queue");
        builder.setMaxSize(500);
        builder.setAccountSid(account);
        builder.setUri(url);

        Queue queue = builder.build();
        final QueuesDao queueDao = manager.getQueuesDao();
        // Create a new application in the data store.
        queueDao.addQueue(queue);
        QueueFilter filter = new QueueFilter(account.toString(), 0, 50);
        assertTrue(queueDao.getQueues(filter).size() == 1);
        // Delete the application.
        queueDao.removeQueues(account);
        assertTrue(queueDao.getQueues(filter).size() == 0);
    }

    @Test
    public void addReadUpdateQueueMembers() {
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.QUEUE);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final Queue.Builder builder = Queue.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Queue");
        builder.setMaxSize(500);
        builder.setAccountSid(account);
        builder.setUri(url);

        Queue queue = builder.build();
        final QueuesDao queueDao = manager.getQueuesDao();
        // Create a new application in the data store.
        queueDao.addQueue(queue);

        // Create three calls queue and save it in data store
        final Sid firstCall = Sid.generate(Sid.Type.CALL);
        final Sid secondCall = Sid.generate(Sid.Type.CALL);
        final Sid thirdCall = Sid.generate(Sid.Type.CALL);

        java.util.Queue<QueueRecord> membersQueue = new LinkedList<QueueRecord>();
        QueueRecord callRecord = new QueueRecord(firstCall.toString(), new Date());
        membersQueue.offer(callRecord);
        callRecord = new QueueRecord(secondCall.toString(), new Date());
        membersQueue.offer(callRecord);

        queueDao.setQueueBytes(membersQueue, queue);

        java.util.Queue<QueueRecord> dbMemberQueue = queueDao.getQueueBytes(sid);

        assertTrue(membersQueue.size() == dbMemberQueue.size());

        // Enqueue new member in existing queue
        callRecord = new QueueRecord(thirdCall.toString(), new Date());
        dbMemberQueue.offer(callRecord);
        queueDao.setQueueBytes(dbMemberQueue, queue);

        dbMemberQueue = queueDao.getQueueBytes(sid);
        // Check positions of queue members
        assertTrue(dbMemberQueue.poll().getCallerSid().equals(firstCall.toString()));
        assertTrue(dbMemberQueue.poll().getCallerSid().equals(secondCall.toString()));
        assertTrue(dbMemberQueue.poll().getCallerSid().equals(thirdCall.toString()));
        assertTrue(dbMemberQueue.isEmpty());

    }

}