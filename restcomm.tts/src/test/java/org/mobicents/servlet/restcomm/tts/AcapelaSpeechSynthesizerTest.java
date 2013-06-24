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
package org.mobicents.servlet.restcomm.tts;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

import java.net.URI;
import java.net.URL;
import java.util.Set;
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
public final class AcapelaSpeechSynthesizerTest {
  private ActorSystem system;
  private ActorRef tts;

  public AcapelaSpeechSynthesizerTest() {
    super();
  }

  @Before public void before() throws Exception {
    system = ActorSystem.create();
	final URL input = getClass().getResource("/acapela.xml");
	final XMLConfiguration configuration = new XMLConfiguration(input);
	tts = tts(configuration);
  }

  @After public void after() throws Exception {
    system.shutdown();
  }
  
  private ActorRef tts(final Configuration configuration) {
    return system.actorOf(new Props(new UntypedActorFactory() {
	  private static final long serialVersionUID = 1L;
	  @Override public Actor create() throws Exception {
		return new AcapelaSpeechSynthesizer(configuration);
	  }
	}));
  }
  
  @SuppressWarnings("unchecked")
  @Test public void testInfo() {
    new JavaTestKit(system) {{
      final ActorRef observer = getRef();
      tts.tell(new GetSpeechSynthesizerInfo(), observer);
      final SpeechSynthesizerResponse<SpeechSynthesizerInfo> response = this.expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
          SpeechSynthesizerResponse.class);
      assertTrue(response.succeeded());
      final Set<String> languages = response.get().languages();
      assertTrue(languages.contains("ca"));
      assertTrue(languages.contains("it"));
      assertTrue(languages.contains("tr"));
      assertTrue(languages.contains("no"));
      assertTrue(languages.contains("ar"));
      assertTrue(languages.contains("cs"));
      assertTrue(languages.contains("de"));
      assertTrue(languages.contains("bp"));
      assertTrue(languages.contains("el"));
      assertTrue(languages.contains("dan"));
      assertTrue(languages.contains("fi"));
      assertTrue(languages.contains("pt"));
      assertTrue(languages.contains("pl"));
      assertTrue(languages.contains("bf"));
      assertTrue(languages.contains("sv"));
      assertTrue(languages.contains("fr"));
      assertTrue(languages.contains("en"));
      assertTrue(languages.contains("ru"));
      assertTrue(languages.contains("es"));
      assertTrue(languages.contains("cf"));
      assertTrue(languages.contains("nl"));
      assertTrue(languages.contains("en-gb"));
      System.out.println(response.get().languages());
    }};
  }

  @SuppressWarnings("unchecked")
  @Test public void testSynthesis() {
    new JavaTestKit(system) {{
      final ActorRef observer = getRef();
      final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest("man", "en", "Hello World!");
      tts.tell(synthesize, observer);
      final SpeechSynthesizerResponse<URI> response = this.expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
          SpeechSynthesizerResponse.class);
      assertTrue(response.succeeded());
      System.out.println(response.get().toString());
    }};
  }
}
