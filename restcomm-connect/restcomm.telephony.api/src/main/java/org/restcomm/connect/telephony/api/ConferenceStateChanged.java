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
package org.restcomm.connect.telephony.api;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public final class ConferenceStateChanged {
    public enum State {
        RUNNING_INITIALIZING, RUNNING_MODERATOR_ABSENT, RUNNING_MODERATOR_PRESENT, STOPPING, COMPLETED, FAILED
    };

    private final String name;
    private final State state;

    public ConferenceStateChanged(final String name, final State state) {
        super();
        this.name = name;
        this.state = state;
    }

    public String name() {
        return name;
    }

    public State state() {
        return state;
    }

    public static State translateState(String stateName, State defaultState) {
        State converetedState = defaultState;
        if(stateName!=null){
            switch (stateName) {
                case "RUNNING_MODERATOR_ABSENT":
                    converetedState=State.RUNNING_MODERATOR_ABSENT;
                    break;

                case "RUNNING_MODERATOR_PRESENT":
                    converetedState=State.RUNNING_MODERATOR_PRESENT;
                    break;

                case "RUNNING_INITIALIZING":
                    converetedState=State.RUNNING_INITIALIZING;
                    break;

                case "COMPLETED":
                    converetedState=State.COMPLETED;
                    break;

                case "FAILED":
                    converetedState=State.FAILED;
                    break;

                case "STOPPING":
                    converetedState=State.STOPPING;
                    break;

                default:
                    break;
            }
        }
        return converetedState;
    }

}
