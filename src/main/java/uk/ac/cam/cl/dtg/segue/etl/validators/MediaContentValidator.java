package uk.ac.cam.cl.dtg.segue.etl.validators;

import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Media;
import uk.ac.cam.cl.dtg.isaac.dos.content.Video;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

/**
 * Validates media content (images, videos) for missing alt text and file existence.
 */
public class MediaContentValidator implements ContentTypeValidator {
  private static final int MEDIA_FILE_SIZE_LIMIT = 300 * 1024;
  private static final String MEDIA = "Media (";
  private final GitDb database;

  MediaContentValidator(final GitDb database) {
    this.database = database;
  }

  @Override
  public void validate(final String sha, final Content content, final IndexingContext context) {
    if (!(content instanceof Media media)) {
      return;
    }

    validateAltText(media, context);
    validateFileExists(sha, media, context);
  }

  private void validateAltText(final Media media, final IndexingContext context) {
    if (!(media instanceof Video) && !"eventThumbnail".equals(media.getId())
        && (media.getAltText() == null || media.getAltText().isEmpty())) {
      context.registerProblem(media, MEDIA + media.getId() + ") is missing alt text. All images must have "
          + "descriptive alt text for accessibility.");
    }
  }

  private void validateFileExists(final String sha, final Media media, final IndexingContext context) {
    String src = media.getSrc();
    if (src == null
        || src.isEmpty()
        || src.startsWith("http://") || src.startsWith("https://") || src.startsWith("/assets/")) {
      return;
    }

    try {
      java.io.ByteArrayOutputStream fileBytes = database.getFileByCommitSha(sha, src);
      if (fileBytes == null) {
        context.registerProblem(media,
            MEDIA + media.getId() + ") references a file that does not exist: " + src);
      } else if (fileBytes.size() > MEDIA_FILE_SIZE_LIMIT) {
        context.registerProblem(media,
            MEDIA + media.getId() + ") file is too large (" + fileBytes.size() + " bytes). "
                + "Maximum size is " + MEDIA_FILE_SIZE_LIMIT + " bytes.");
      }
    } catch (Exception e) {
      context.registerProblem(media,
          MEDIA + media.getId() + ") file check failed: " + e.getMessage());
    }
  }
}
