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
package org.mobicents.servlet.restcomm.http.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import scala.concurrent.duration.FiniteDuration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class DownloaderTest {
    private ActorSystem system;
    private ActorRef downloader;

    public DownloaderTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        downloader = system.actorOf(new Props(Downloader.class));
    }

    @After
    public void after() throws Exception {
        system.shutdown();
    }

    @Test
    public void testGet() throws URISyntaxException, IOException {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                final URI uri = URI.create("http://www.restcomm.org");
                final String method = "GET";
                final HttpRequestDescriptor request = new HttpRequestDescriptor(uri, method);
                downloader.tell(request, observer);
                final FiniteDuration timeout = FiniteDuration.create(30, TimeUnit.SECONDS);
                final DownloaderResponse response = expectMsgClass(timeout, DownloaderResponse.class);
                assertTrue(response.succeeded());
                final HttpResponseDescriptor descriptor = response.get();
                System.out.println("Result: " + descriptor.getContentAsString());
                assertTrue(descriptor.getContentAsString().contains("<title>RestComm - Home</title>"));
            }
        };
    }

    @Test
    @Ignore
    public void testPost() throws URISyntaxException, IOException {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                final URI uri = URI.create("http://www.restcomm.org");
                final String method = "POST";
                final HttpRequestDescriptor request = new HttpRequestDescriptor(uri, method);
                downloader.tell(request, observer);
                final FiniteDuration timeout = FiniteDuration.create(30, TimeUnit.SECONDS);
                final DownloaderResponse response = expectMsgClass(timeout, DownloaderResponse.class);
                assertTrue(response.succeeded());
                final HttpResponseDescriptor descriptor = response.get();
                assertTrue(descriptor.getContentAsString().contains("<title>RestComm - Home</title>"));
            }
        };
    }

    @Test @Ignore
    public void testNotFound() throws URISyntaxException, IOException {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                final URI uri = URI.create("http://www.telestax.com/not-found.html");
                final String method = "GET";
                final HttpRequestDescriptor request = new HttpRequestDescriptor(uri, method);
                downloader.tell(request, observer);
                final FiniteDuration timeout = FiniteDuration.create(30, TimeUnit.SECONDS);
                final DownloaderResponse response = expectMsgClass(timeout, DownloaderResponse.class);
                assertTrue(response.succeeded());
                final HttpResponseDescriptor descriptor = response.get();
                assertTrue(descriptor.getStatusCode() == 404);
            }
        };
    }
}
