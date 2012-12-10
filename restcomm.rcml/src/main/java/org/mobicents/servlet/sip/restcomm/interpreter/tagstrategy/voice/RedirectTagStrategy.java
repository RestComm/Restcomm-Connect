package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice;

import java.net.URI;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.VoiceRcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

public class RedirectTagStrategy extends VoiceRcmlTagStrategy {
  private static final Logger logger = Logger.getLogger(RedirectTagStrategy.class);

  private String method;
  private URI uri;

  public RedirectTagStrategy() {
    super();
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    // Redirect the interpreter to the new RCML resource.
    if(uri != null) {
      final VoiceRcmlInterpreterContext voiceContext = (VoiceRcmlInterpreterContext)context;
      if(Call.Status.IN_PROGRESS.equals(voiceContext.getCall().getStatus())) {
        try {
          interpreter.load(uri, method, context.getRcmlRequestParameters());
          interpreter.redirect();
        } catch(final InterpreterException exception) {
          interpreter.failed();
          final Notification notification = interpreter.notify(context, Notification.ERROR, 12400);
          interpreter.save(notification);
          logger.error(exception);
          throw new TagStrategyException(exception);
        }
      }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	super.initialize(interpreter, context, tag);
    initMethod(interpreter, context, tag);
    initUri(interpreter, context, tag);
  }
  
  private void initMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    method = getMethod(interpreter, context, tag);
	if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
	  final Notification notification = interpreter.notify(context, Notification.WARNING, 13710);
	  interpreter.save(notification);
	  method = "POST";
	}
  }
  
  private void initUri(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
      uri = getUri(interpreter, context, tag);
    } catch(final IllegalArgumentException ignored) {
      interpreter.failed();
      final Notification notification = interpreter.notify(context, Notification.ERROR, 11100);
      interpreter.save(notification);
      throw new TagStrategyException(tag.getText() + " is an invalid URI.");
    }
  }  
}
