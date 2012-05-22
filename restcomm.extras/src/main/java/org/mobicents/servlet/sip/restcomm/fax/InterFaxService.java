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
package org.mobicents.servlet.sip.restcomm.fax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.rpc.ServiceException;

import net.interfax.outbound.InterFaxLocator;
import net.interfax.outbound.InterFaxSoapStub;
import net.interfax.outbound.Sendfax;
import net.interfax.outbound.SendfaxResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class InterFaxService implements FaxService, Runnable {
  private static final Logger logger = Logger.getLogger(InterFaxService.class);
  private static final Set<String> extensions;
  static {
    extensions = new HashSet<String>();
    extensions.add("ODF");
    extensions.add("ODG");
    extensions.add("ODM");
    extensions.add("ODP");
    extensions.add("ODS");
    extensions.add("ODT");
    extensions.add("DOC");
    extensions.add("DOCX");
    extensions.add("DOT");
    extensions.add("XML");
    extensions.add("PDF");
    extensions.add("TIF");
    extensions.add("TXT");
    extensions.add("XLS");
    extensions.add("XLSX");
    extensions.add("HTM");
    extensions.add("HTML");
    extensions.add("MHT");
    extensions.add("URL");
    extensions.add("PPT");
    extensions.add("PPTX");
    extensions.add("GIF");
    extensions.add("PNG");
    extensions.add("JPG");
    extensions.add("JPEG");
    extensions.add("PS");
    extensions.add("EPS");
    extensions.add("RTF");
    extensions.add("BMP");
    extensions.add("PCX");
    extensions.add("SNP");
    extensions.add("ZIP");
  }
  
  private Configuration configuration;
  private String user;
  private String password;
  private long maxFileSize;
  
  private InterFaxSoapStub interfax;
  
  private Thread worker;
  private volatile boolean running;
  private BlockingQueue<FaxRequest> queue;
  
  public InterFaxService() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  private final String getFileExtension(final URI uri) {
    final String path = uri.getPath();
    final int indexOfSeparator = path.lastIndexOf(".");
    if(indexOfSeparator > -1) {
      return path.substring(indexOfSeparator + 1, path.length());
    } else {
      return null;
    }
  }
  
  @Override public void run() {
    while(running) {
      FaxRequest request = null;
      try { request = queue.take(); }
      catch(final InterruptedException ignored) { }
      if(request != null) {
        try {
          final Sendfax fax = new Sendfax(user, password, request.getTo(),
              toBytes(request.getContent()), "");
          final SendfaxResponse response = interfax.sendfax(fax);
          final long result = response.getSendfaxResult();
          if(result < 0) {
            request.getObserver().failed();
          } else {
            request.getObserver().succeeded();
          }
        } catch(final IOException exception) {
          logger.error(exception);
          request.getObserver().failed();
        }
      }
    }
  }

  @Override public void start() throws RuntimeException {
    user = configuration.getString("user");
    password = configuration.getString("password");
    maxFileSize = configuration.getLong("maximum-file-size");
    final int timeout = configuration.getInt("timeout");
    try {
      interfax = (InterFaxSoapStub)new InterFaxLocator().getInterFaxSoap();
      interfax.setTimeout((int)(timeout * TimeUtils.SECOND_IN_MILLIS));
    } catch(final ServiceException exception) {
      throw new RuntimeException(exception);
    }
    queue = new LinkedBlockingQueue<FaxRequest>();
    worker = new Thread(this);
    worker.setName("InterFax Worker");
    running = true;
    worker.start();
  }

  @Override public void shutdown() {
    if(running) {
      running = false;
      worker.interrupt();
    }
  }

  @Override public void send(final String from, final String to, final URI content,
      final FaxServiceObserver observer) throws FaxServiceException {
    if(extensions.contains(getFileExtension(content).toUpperCase())) {
      try { queue.put(new FaxRequest(to, content, observer)); }
      catch(final InterruptedException ignored) { }
    } else {
      throw new FaxServiceException("The content is not a supported media type.");
    }
  }
  
  private final byte[] toBytes(final URI uri) throws IOException {
    final InputStream input = uri.toURL().openStream();
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      final byte[] buffer = new byte[1024];
      long totalBytesRead = 0;
      int bytesRead = 0;
      do {
        bytesRead = input.read(buffer, 0, 1024);
        if(bytesRead > 0) {
          output.write(buffer, 0, bytesRead);
          totalBytesRead += bytesRead;
          if(totalBytesRead > maxFileSize) {
            final StringBuilder exception = new StringBuilder();
            exception.append("The size of the resource located @ ").append(uri.toString())
                .append(" is to large.");
            throw new IOException(exception.toString());
          }
        }
      } while(bytesRead != -1);
      return output.toByteArray();
    } finally {
      input.close();
    }
  }
  
  @Immutable private final class FaxRequest {
    private final String to;
    private final URI content;
    private final FaxServiceObserver observer;
    
    private FaxRequest(final String to, final URI content, final FaxServiceObserver observer) {
      super();
      this.to = to;
      this.content = content;
      this.observer = observer;
    }

	public String getTo() {
	  return to;
	}

	public URI getContent() {
	  return content;
	}

	public FaxServiceObserver getObserver() {
	  return observer;
	}
  }
}
