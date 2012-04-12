package org.mobicents.servlet.sip.restcomm.asr;

public interface SpeechRecognizerObserver {
  public void succeeded(String text);
  public void failed();
}
