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
package org.mobicents.servlet.sip.restcomm.resourceserver;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.xml.XmlResource;
import org.mobicents.servlet.sip.restcomm.xml.XmlResourceBuilder;
import org.mobicents.servlet.sip.restcomm.xml.XmlResourceBuilderException;
import org.mobicents.servlet.sip.restcomm.xml.TagFactory;
import org.mobicents.servlet.sip.restcomm.xml.twiml.TwiMLTagFactory;

public final class RestCommResourceServer implements ResourceServer {
  private static final Logger LOGGER = Logger.getLogger(RestCommResourceServer.class);
  private final Map<String, SchemeStrategy> strategies;

  public RestCommResourceServer() {
    super();
    strategies = new HashMap<String, SchemeStrategy>();
  }
  
  public void addSchemeStrategy(final SchemeStrategy strategy) {
    if(!strategies.containsKey(strategy.getScheme())) {
      strategies.put(strategy.getScheme(), strategy);
    }
  }

  public XmlResource getXmlResource(final ResourceDescriptor descriptor) throws ResourceFetchException {
    final URI uri = descriptor.getUri();
    final String scheme = uri.getScheme();
    if(strategies.containsKey(scheme)) {
      if(LOGGER.isDebugEnabled()) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Fetching an XML resource located at ").append(uri).append(" using the HTTP ");
        buffer.append(descriptor.getMethod()).append(" method.");
        LOGGER.debug(buffer.toString());
      }
      final SchemeStrategy strategy = strategies.get(scheme);
      final Map<String, Object> attributes = descriptor.getAttributes();
      final byte[] message = descriptor.getMessage();
      final String method = descriptor.getMethod();
      final InputStream input = strategy.getInputStream(uri, attributes, message, method);
      final TagFactory tagFactory = new TwiMLTagFactory();
      final XmlResourceBuilder resourceBuilder = new XmlResourceBuilder(tagFactory);
      try {
        final XmlResource resource = resourceBuilder.build(input);
        if(LOGGER.isDebugEnabled()) {
          final StringBuilder buffer = new StringBuilder();
          buffer.append("The fetch for ").append(uri).append(" returned the following resource:\n");
          buffer.append(resource);
          LOGGER.debug(buffer.toString());
        }
        return resource;
      } catch(final XmlResourceBuilderException exception) {
        throw new ResourceFetchException(exception);
      }
    } else {
      throw new ResourceFetchException(scheme + " is not a supported scheme.");
    }
  }
}
