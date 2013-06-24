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
package org.mobicents.servlet.restcomm.fax;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;
//import akka.testkit.JavaTestKit;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import scala.concurrent.duration.FiniteDuration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class InterfaxServiceTest {
  private ActorSystem system;
  private ActorRef interfax;

  public InterfaxServiceTest() {
    super();
  }

  @Before public void before() throws Exception {
    system = ActorSystem.create();
    final URL input = getClass().getResource("/interfax.xml");
	final XMLConfiguration configuration = new XMLConfiguration(input);
	interfax = interfax(configuration);
  }

  @After public void after() throws Exception {
    system.shutdown();
  }
  
  private ActorRef interfax(final Configuration configuration) {
    return system.actorOf(new Props(new UntypedActorFactory() {
	  private static final long serialVersionUID = 1L;
	  @Override public Actor create() throws Exception {
		return new InterfaxService(configuration);
	  }
	}));
  }

  @Test	public void testSendFax() {
    new JavaTestKit(system) {{
      final ActorRef observer = getRef();
      final File file = new File(getClass().getResource("/fax.pdf").getPath());
      // This is the fax number for http://faxtoy.net/
      final FaxRequest request = new FaxRequest("+18888771655", file);
      // This will fax "Welcome to RestComm!" to http://faxtoy.net/
      interfax.tell(request, observer);
      final FaxResponse response = expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
          FaxResponse.class);
      assertTrue(response.succeeded());
      System.out.println(response.get().toString());
    }};
  }
}
