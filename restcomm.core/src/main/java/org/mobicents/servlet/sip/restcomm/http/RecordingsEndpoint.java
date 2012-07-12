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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.thoughtworks.xstream.XStream;

import java.util.List;

import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.sip.restcomm.entities.Recording;
import org.mobicents.servlet.sip.restcomm.entities.RecordingList;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.http.converter.RecordingConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RecordingListConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class RecordingsEndpoint extends AbstractEndpoint {
  protected final RecordingsDao dao;
  protected final Gson gson;
  protected final XStream xstream;

  public RecordingsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getRecordingsDao();
    final RecordingConverter converter = new RecordingConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Recording.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new RecordingListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
  
  protected Response getRecording(final String accountSid, final String sid,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Recordings"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Recording recording = dao.getRecording(new Sid(sid));
    if(recording == null) {
      return status(NOT_FOUND).build();
    } else {
      if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(recording), APPLICATION_JSON).build();
      } else if(APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(recording);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getRecordings(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Recordings"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Recording> recordings = dao.getRecordings(new Sid(accountSid));
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(recordings), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new RecordingList(recordings));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
}
