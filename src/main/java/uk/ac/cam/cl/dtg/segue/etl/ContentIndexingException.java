package uk.ac.cam.cl.dtg.segue.etl;

/**
 * Exception thrown when content indexing fails.
 */
public class ContentIndexingException extends RuntimeException {

  /**
   * Constructs a new ContentIndexingException with the specified detail message.
   *
   * @param message the detail message
   */
  public ContentIndexingException(final String message) {
    super(message);
  }

  /**
   * Constructs a new ContentIndexingException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public ContentIndexingException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new ContentIndexingException with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public ContentIndexingException(final Throwable cause) {
    super(cause);
  }
}