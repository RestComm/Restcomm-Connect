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
package org.mobicents.servlet.sip.restcomm.http;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public abstract class AbstractEndpointTest {
  @ArquillianResource protected Deployer deployer;
  private static final String projects = "/home/thomas/Projects";
  
  public AbstractEndpointTest() {
    super();
  }
  
  @Deployment(name="restcomm", managed=false, testable=false)
  public static WebArchive createTestArchive() {
    DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
    final File directory = new File(projects + "/RestComm/restcomm/restcomm.core/target/restcomm/");
    // Load archive from exploded directory.
    WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
    archive.as(ExplodedImporter.class).importDirectory(directory);
    return archive;
  }
}
