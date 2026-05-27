package uk.ac.cam.cl.dtg.segue.etl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;

public record IndexingContext(Map<String, Content> contentCache, Set<String> tagsList, Map<String, String> allUnits,
                              Map<String, String> publishedUnits, Map<Content, List<String>> indexProblemCache,
                              boolean includeUnpublished) {

  public boolean shouldSkipUnpublished(final Content content) {
    return !includeUnpublished && !content.getPublished();
  }
}
