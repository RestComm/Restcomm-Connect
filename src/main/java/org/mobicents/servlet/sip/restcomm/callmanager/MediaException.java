package org.mobicents.servlet.sip.restcomm.callmanager;

public final class MediaException extends Exception {
  private static final long serialVersionUID = 1L;

  public MediaException() {
    super();
  }

  public MediaException(final String message) {
    super(message);
  }

  public MediaException(final Throwable cause) {
    super(cause);
  }

  public MediaException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
