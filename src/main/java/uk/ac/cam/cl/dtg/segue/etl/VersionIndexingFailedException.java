package uk.ac.cam.cl.dtg.segue.etl;

public class VersionIndexingFailedException extends Exception {
  public VersionIndexingFailedException(final String version) {
    super("Failed to index content version '" + version + "': not all content types were successfully indexed");
  }

  public VersionIndexingFailedException(final String version, final Throwable cause) {
    super("Failed to index content version '" + version + "': " + cause.getMessage(), cause);
  }
}