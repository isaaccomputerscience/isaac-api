/**
 * Copyright 2021 Raspberry Pi Foundation
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.inject.Inject;
import java.util.ArrayList;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.ContentMapper;

public class ContentSummarizerService {
  private final ContentMapper mapper;
  private final URIManager uriManager;

  @Inject
  public ContentSummarizerService(final ContentMapper mapper, final URIManager uriManager) {
    this.mapper = mapper;
    this.uriManager = uriManager;
  }

  /**
   * Simplify a ContentDTO object to a smaller summary class to reduce the size of the response.
   *
   * @param content      - the content object to summarise
   * @param summaryClass - the subclass of ContentSummaryDTO to use for the summary object, to allow flexibility
   * @return ContentSummaryDTO.
   */
  public ContentSummaryDTO extractContentSummary(final ContentDTO content,
                                                 final Class<? extends ContentSummaryDTO> summaryClass) {
    if (null == content) {
      return null;
    }

    // try auto-mapping
    ContentSummaryDTO contentInfo = mapper.map(content, summaryClass);
    contentInfo.setUrl(uriManager.generateApiUrl(content));

    return contentInfo;
  }

  /**
   * Helper method to simplify a ContentDTO object directly to ContentSummaryDTO.
   *
   * @param content - the content data transfer object
   * @return the ContentSummaryDTO generated from the provided ContentDTO
   * @see ContentSummarizerService#extractContentSummary(ContentDTO, Class)
   */
  public ContentSummaryDTO extractContentSummary(final ContentDTO content) {
    return extractContentSummary(content, ContentSummaryDTO.class);
  }

  /**
   * Utility method to convert a ResultsWrapper of content objects into one with ContentSummaryDTO objects.
   *
   * @param contentList  - the list of content to summarise.
   * @param summaryClass - the subclass of ContentSummaryDTO to convert the content into
   * @return list of shorter ContentSummaryDTO objects.
   */
  public ResultsWrapper<ContentSummaryDTO> extractContentSummaryFromResultsWrapper(
      final ResultsWrapper<ContentDTO> contentList, final Class<? extends ContentSummaryDTO> summaryClass) {
    if (null == contentList) {
      return null;
    }

    ResultsWrapper<ContentSummaryDTO> contentSummaryResults =
        new ResultsWrapper<>(new ArrayList<>(), contentList.getTotalResults());

    for (ContentDTO content : contentList.getResults()) {
      ContentSummaryDTO contentInfo = extractContentSummary(content, summaryClass);
      if (null != contentInfo) {
        contentSummaryResults.getResults().add(contentInfo);
      }
    }
    return contentSummaryResults;
  }

  /**
   * Helper method to simplify a ResultsWrapper of ContentDTO objects directly to ContentSummaryDTOs.
   *
   * @param contentList - the list of content to summarise.
   * @return list of shorter ContentSummaryDTO objects.
   * @see ContentSummarizerService#extractContentSummaryFromResultsWrapper(ResultsWrapper, Class)
   */
  public ResultsWrapper<ContentSummaryDTO> extractContentSummaryFromResultsWrapper(
      final ResultsWrapper<ContentDTO> contentList) {
    return extractContentSummaryFromResultsWrapper(contentList, ContentSummaryDTO.class);
  }
}
