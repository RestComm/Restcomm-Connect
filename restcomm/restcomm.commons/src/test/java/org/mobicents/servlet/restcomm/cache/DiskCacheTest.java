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
package org.mobicents.servlet.restcomm.cache;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class DiskCacheTest {
    private ActorSystem system;
    private ActorRef cache;

    public DiskCacheTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        cache = cache("/tmp", "http://127.0.0.1:8080/restcomm/cache");
    }

    @After
    public void after() throws Exception {
        system.shutdown();
    }

    private ActorRef cache(final String location, final String uri) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new DiskCache(location, uri);
            }
        }));
    }

    @Test
    public void test() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                cache.tell(new DiskCacheRequest(URI.create("http://www.mobicents.org/index.html")), observer);
                final DiskCacheResponse response = this.expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
                        DiskCacheResponse.class);
                assertTrue(response.succeeded());
                final File file = new File("/tmp/0877edd3c836d7b38d9615939ff66254fab90b868d75e1b86fcc8d42682d9741.html");
                assertTrue(file.exists());
                assertTrue(file.length() > 0);
                final URI result = response.get();
                final URI uri = URI
                        .create("http://127.0.0.1:8080/restcomm/cache/0877edd3c836d7b38d9615939ff66254fab90b868d75e1b86fcc8d42682d9741.html");
                assertTrue(result.equals(uri));
            }
        };
    }

    @Test
    public void testHashInTheURI() {
        final String uriStr = "http://www.mobicents.org/index.html#hash=c32735f2960ef70edb5116e8459c5b65915b3c27ea4773feaa35ce55220f4755";

        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                cache.tell(new DiskCacheRequest(URI.create(uriStr)), observer);
                final DiskCacheResponse response = this.expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
                        DiskCacheResponse.class);
                assertTrue(response.succeeded());
                final File file = new File("/tmp/c32735f2960ef70edb5116e8459c5b65915b3c27ea4773feaa35ce55220f4755.html");
                assertTrue(file.exists());
                assertTrue(file.length() > 0);
                final URI result = response.get();
                final URI uri = URI
                        .create("http://127.0.0.1:8080/restcomm/cache/c32735f2960ef70edb5116e8459c5b65915b3c27ea4773feaa35ce55220f4755.html");
                assertTrue(result.equals(uri));
            }
        };
    }
}
