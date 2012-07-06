package org.mobicents.servlet.sip.restcomm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twilio.sdk.verbs.Dial;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

public final class SayVerbRcmlTest extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public SayVerbRcmlTest() {
    super();
  }

  @Override protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    try {
      final Dial dial = new Dial("+15126002188");
      dial.setCallerId("+13055872294");
      final TwiMLResponse rcml = new TwiMLResponse();
      rcml.append(dial);
      response.getWriter().write(rcml.toXML());
    } catch(final TwiMLException exception) {
      throw new ServletException(exception);
    }
  }
}
