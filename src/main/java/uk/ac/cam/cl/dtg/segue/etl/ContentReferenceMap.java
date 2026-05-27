package uk.ac.cam.cl.dtg.segue.etl;

import java.util.Map;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;

public record ContentReferenceMap(Set<String> expectedIds, Map<String, Set<Content>> incomingReferences) {
}