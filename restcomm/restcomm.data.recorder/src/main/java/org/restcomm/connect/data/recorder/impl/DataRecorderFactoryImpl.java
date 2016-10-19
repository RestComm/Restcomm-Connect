/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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
package org.restcomm.connect.data.recorder.impl;

import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.data.recorder.CallDataRecorderImpl;
import org.restcomm.connect.data.recorder.ConferenceDataRecorderImpl;
import org.restcomm.connect.data.recorder.SMSDataRecorderImpl;
import org.restcomm.connect.data.recorder.USSDDataRecorderImpl;
import org.restcomm.connect.data.recorder.api.DataRecorderFactory;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

public class DataRecorderFactoryImpl implements DataRecorderFactory {

    private final ActorSystem system;
    private final DaoManager daoManager;
    private final CallDataRecorderFactory callDataRecorderFactory;
    private final ConferenceDataRecorderFactory conferenceDataRecorderFactory;
    private final SmsDataRecorderFactory smsDataRecorderFactory;
    private final UssdDataRecorderFactory ussdDataRecorderFactory;

    public DataRecorderFactoryImpl(ActorSystem system, final DaoManager daoManager) {
        super();
        this.system = system;
        this.daoManager = daoManager;
        callDataRecorderFactory = new CallDataRecorderFactory();
        conferenceDataRecorderFactory =new  ConferenceDataRecorderFactory();
        smsDataRecorderFactory = new SmsDataRecorderFactory();
        ussdDataRecorderFactory = new UssdDataRecorderFactory();
    }

    private final class CallDataRecorderFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4649683839304615852L;

        @Override
        public Actor create() throws Exception {
            return new CallDataRecorderImpl(daoManager);
        }

    }

    private final class ConferenceDataRecorderFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4649683839304615853L;

        @Override
        public Actor create() throws Exception {
            return new ConferenceDataRecorderImpl(daoManager);
        }

    }

    private final class SmsDataRecorderFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4649683839304615854L;

        @Override
        public Actor create() throws Exception {
            return new SMSDataRecorderImpl(daoManager);
        }

    }

    private final class UssdDataRecorderFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4649683839304615855L;

        @Override
        public Actor create() throws Exception {
            return new USSDDataRecorderImpl(daoManager);
        }

    }

    @Override
    public ActorRef getCallDataRecorder() {
        return system.actorOf(new Props(this.callDataRecorderFactory));
    }

    @Override
    public ActorRef getConferenceDataRecorder() {
        return system.actorOf(new Props(this.conferenceDataRecorderFactory));
    }

    @Override
    public ActorRef getSMSDataRecorder() {
        return system.actorOf(new Props(this.smsDataRecorderFactory));
    }

    @Override
    public ActorRef getUSSDDataRecorder() {
        return system.actorOf(new Props(this.ussdDataRecorderFactory));
    }

}
