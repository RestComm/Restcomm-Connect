package org.mobicents.servlet.sip.restcomm.asr;

import java.io.Serializable;

public interface SpeechRecognizerObserver {
  public void succeeded(String text, Serializable object);
  public void failed(Serializable Object);
}
