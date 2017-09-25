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

package org.restcomm.connect.interpreter.rcml;

import akka.actor.*;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.mockito.stubbing.Answer1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gdubina on 6/24/17.
 */
public class MockedActor {

    private final Map<Class<?>, List<Object>> mapping;
    private final String name;

    public MockedActor(String name, Map<Class<?>, List<Object>> mapping) {
        super();
        this.name = name;
        this.mapping = mapping;
    }

    public MockedActor(String name) {
        this(name, new HashMap<Class<?>, List<Object>>());
    }

    public void onReceive(ActorRef self, ActorRef sender, Object msg) throws Throwable {
        List<Object> responses = this.mapping.get(msg.getClass());
        if (responses == null || responses.isEmpty()) {
            System.err.println("MockedActor." + name + ": unhandled request - " + msg);
            return;
        }
        for (Object response : responses) {
            if (response instanceof Answer1) {
                response = ((Answer1) response).answer(msg);
                if (response instanceof Optional) {
                    Optional resp = ((Optional) response);
                    if (resp.isPresent()) {
                        response = resp.get();
                    } else {
                        continue;
                    }
                }
            }

            sender.tell(response, self);
            return;
        }
    }

    public MockedActor add(Class<?> clazz, Object resp) {
        List<Object> responses = mapping.get(clazz);
        if (responses == null) {
            mapping.put(clazz, responses = new ArrayList<>());
        }
        responses.add(resp);
        return this;
    }

    public MockedActor add(final Class<?> request, final Predicate<Object> matcher, final Object resp) {
        this.add(request, new Answer1<Optional, Object>() {
            @Override
            public Optional answer(Object msg) throws Throwable {
                if (matcher.apply(msg)) {
                    return Optional.fromNullable(resp);
                }
                return Optional.absent();
            }
        });
        return this;
    }

    private class MockedActorRef extends UntypedActor {

        public MockedActorRef() {

        }

        @Override
        public void onReceive(Object o) throws Exception {
            try {
                MockedActor.this.onReceive(self(), sender(), o);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    public ActorRef asRef(ActorSystem system) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new MockedActorRef();
            }
        }));
    }

    public static class SimplePropertyPredicate<T, P> implements Predicate<Object> {

        private final P value;
        private final Function<T, P> extractor;

        public SimplePropertyPredicate(P value, Function<T, P> extractor) {
            this.value = value;
            this.extractor = extractor;
        }

        @Override
        public boolean apply(Object t) {
            return value.equals(extractor.apply((T) t));
        }
    }
}
