package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.List;

public record SyncResult(
    List<String> failedUserDetails,
    int successCount,
    int totalCount
) {
  public boolean hasFailures() {
    return !failedUserDetails.isEmpty();
  }
}
