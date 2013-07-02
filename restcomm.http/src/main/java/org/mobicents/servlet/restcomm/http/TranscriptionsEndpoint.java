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
package org.mobicents.servlet.restcomm.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.thoughtworks.xstream.XStream;

import java.util.List;

import static javax.ws.rs.core.MediaType.*;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Transcription;
import org.mobicents.servlet.restcomm.entities.TranscriptionList;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.http.converter.TranscriptionConverter;
import org.mobicents.servlet.restcomm.http.converter.TranscriptionListConverter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class TranscriptionsEndpoint extends AbstractEndpoint {
  @javax.ws.rs.core.Context 
  private ServletContext context;
  protected final TranscriptionsDao dao;
  protected final Gson gson;
  protected final XStream xstream;
  
  public TranscriptionsEndpoint() {
    super();
    final DaoManager storage = (DaoManager)context.getAttribute(DaoManager.class.getName());
    dao = storage.getTranscriptionsDao();
    final TranscriptionConverter converter = new TranscriptionConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Transcription.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new TranscriptionListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
  
  protected Response getTranscription(final String accountSid, final String sid,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Transcriptions"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Transcription transcription = dao.getTranscription(new Sid(sid));
    if(transcription == null) {
      return status(NOT_FOUND).build();
    } else {
      if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(transcription), APPLICATION_JSON).build();
      } else if(APPLICATION_XML_TYPE == responseType) {
    	final RestCommResponse response = new RestCommResponse(transcription);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      }  else {
        return null;
      }
    }
  }
  
  protected Response getTranscriptions(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Transcriptions"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Transcription> transcriptions = dao.getTranscriptions(new Sid(accountSid));
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(transcriptions), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new TranscriptionList(transcriptions));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    }  else {
      return null;
    }
  }
}
