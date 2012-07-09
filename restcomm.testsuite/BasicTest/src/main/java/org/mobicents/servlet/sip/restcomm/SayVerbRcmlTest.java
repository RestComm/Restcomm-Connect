package org.mobicents.servlet.sip.restcomm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twilio.sdk.verbs.Say;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

public final class SayVerbRcmlTest extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public SayVerbRcmlTest() {
    super();
  }

  @Override protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    final TwiMLResponse rcml = new TwiMLResponse();
    final Say say = new Say("Hello World");
    say.setVoice("man");
    say.setLoop(1);
    try { rcml.append(say); }
    catch(final TwiMLException exception) { throw new ServletException(exception); }
    response.getWriter().write(rcml.toXML());
  }
}
