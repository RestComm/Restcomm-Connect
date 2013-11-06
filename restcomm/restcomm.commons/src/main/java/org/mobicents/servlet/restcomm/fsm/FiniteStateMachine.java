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
package org.mobicents.servlet.restcomm.fsm;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public class FiniteStateMachine {
    private final ImmutableMap<State, Map<State, Transition>> transitions;
    private State state;

    public FiniteStateMachine(final State initial, final Set<Transition> transitions) {
        super();
        checkNotNull(initial, "The initial state for a finite state machine can not be null.");
        checkNotNull(transitions, "A finite state machine can not be created with transitions set to null.");
        this.state = initial;
        this.transitions = toImmutableMap(transitions);
    }

    public State state() {
        return state;
    }

    public void transition(final Object event, final State target) throws TransitionFailedException,
            TransitionNotFoundException, TransitionRollbackException {
        checkNotNull(event, "The message passed can not be null.");
        checkNotNull(target, "The target state can not be null");
        if (transitions.get(state) == null || !transitions.get(state).containsKey(target)) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("No transition could be found from a(n) ").append(state.getId()).append(" state to a(n) ")
                    .append(target.getId()).append(" state.");
            throw new TransitionNotFoundException(buffer.toString(), event, state, target);
        }
        final Transition transition = transitions.get(state).get(target);
        final Guard guard = transition.getGuard();
        boolean accept = true;
        if (guard != null) {
            try {
                accept = guard.accept(event, transition);
            } catch (final Exception exception) {
                throw new TransitionFailedException(exception, event, transition);
            }
        }
        if (accept) {
            final Action actionOnExit = state.getActionOnExit();
            if (actionOnExit != null) {
                try {
                    actionOnExit.execute(event);
                } catch (final Exception exception) {
                    throw new TransitionFailedException(exception, event, transition);
                }
            }
            final Action actionOnEnter = target.getActionOnEnter();
            if (actionOnEnter != null) {
                try {
                    actionOnEnter.execute(event);
                } catch (final Exception exception) {
                    throw new TransitionFailedException(exception, event, transition);
                }
            }
        } else {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("The condition guarding a transition from a(n) ").append(transition.getStateOnEnter().getId())
                    .append(" state to a(n) ").append(transition.getStateOnExit().getId()).append(" state has failed.");
            throw new TransitionRollbackException(buffer.toString(), event, transition);
        }
        state = target;
    }

    private ImmutableMap<State, Map<State, Transition>> toImmutableMap(final Set<Transition> transitions) {
        final Map<State, Map<State, Transition>> map = new HashMap<State, Map<State, Transition>>();
        for (final Transition transition : transitions) {
            final State stateOnEnter = transition.getStateOnEnter();
            if (!map.containsKey(stateOnEnter)) {
                map.put(stateOnEnter, new HashMap<State, Transition>());
            }
            final State stateOnExit = transition.getStateOnExit();
            map.get(stateOnEnter).put(stateOnExit, transition);
        }
        return ImmutableMap.copyOf(map);
    }
}
