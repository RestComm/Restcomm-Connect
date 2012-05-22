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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.net.URI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.fax.FaxService;
import org.mobicents.servlet.sip.restcomm.fax.FaxServiceException;
import org.mobicents.servlet.sip.restcomm.fax.FaxServiceObserver;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.From;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.To;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class FaxTagStrategy extends RcmlTagStrategy implements FaxServiceObserver {
  private static final Logger logger = Logger.getLogger(FaxTagStrategy.class);
  private final FaxService faxService;

  private PhoneNumber from;
  private PhoneNumber to;
  private URI statusCallback;
  private URI uri;
  
  public FaxTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    faxService = services.get(FaxService.class);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      faxService.send(phoneNumberUtil.format(from, PhoneNumberFormat.E164),
          phoneNumberUtil.format(to, PhoneNumberFormat.E164), uri, this);
    } catch(final FaxServiceException exception) {
      interpreter.failed();
  	  interpreter.notify(context, Notification.ERROR, 12400);
  	  logger.error(exception);
      throw new TagStrategyException(exception);
    }
  }
  
  @Override public void failed() {
    handleFax(false);
  }
  
  private PhoneNumber getFrom(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final PhoneNumber phoneNumber = getPhoneNumber(interpreter, context, tag, From.NAME);
    if(phoneNumber != null) {
      return phoneNumber;
    } else {
      interpreter.notify(context, Notification.WARNING, 15102);
      return null;
    }
  }
  
  private PhoneNumber getTo(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final PhoneNumber phoneNumber = getPhoneNumber(interpreter, context, tag, To.NAME);
    if(phoneNumber != null) {
      return phoneNumber;
    } else {
      interpreter.notify(context, Notification.WARNING, 15101);
      return null;
    }
  }
  
  private void handleFax(final boolean success) {
    if(success) {
      System.out.println("***************** Success! *****************");
    } else {
      System.out.println("***************** Failure! *****************");
    }
    if(statusCallback != null) {
      
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    from = getFrom(interpreter, context, tag);
    to = getTo(interpreter, context, tag);
    statusCallback = getStatusCallback(interpreter, context, tag);
    initUri(interpreter, context, tag);
  }
  
  private void initUri(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
      uri = getUri(interpreter, context, tag);
      if(uri == null) {
    	interpreter.failed();
        interpreter.notify(context, Notification.ERROR, 15120);
        throw new TagStrategyException("There is no resource to fax.");
      }
    } catch(final IllegalArgumentException exception) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 11100);
      throw new TagStrategyException(tag.getText() + " is an invalid URI.");
    }
  }

  @Override public void succeeded() {
    handleFax(true);
  }
}
