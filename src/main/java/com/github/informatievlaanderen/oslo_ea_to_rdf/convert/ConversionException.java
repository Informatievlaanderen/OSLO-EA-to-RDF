package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

/**
 * An exception that indicates a conversion problem.
 *
 * @author Dieter De Paepe
 */
public class ConversionException extends Exception {
  public ConversionException() {}

  public ConversionException(String message) {
    super(message);
  }

  public ConversionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConversionException(Throwable cause) {
    super(cause);
  }

  public ConversionException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
