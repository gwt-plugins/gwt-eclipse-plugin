package com.google.gdt.eclipse.core;

/**
 * Exception thrown upon an attempt to run GPE on a backlevel JVM or with a backlevel JRE.
 */
@SuppressWarnings("serial")
public class GpeVersionException extends Exception {

  public GpeVersionException(String message) {
    super(message);
  }

  public GpeVersionException(String message, Throwable cause) {
    super(message, cause);
  }

}
